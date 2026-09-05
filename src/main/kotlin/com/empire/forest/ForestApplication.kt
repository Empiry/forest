package com.empire.forest

import co.aikar.commands.BaseCommand
import com.empire.forest.command.*
import com.empire.forest.config.ForestInteractiveData
import com.empire.forest.constants.ForestConstants
import com.empire.forest.constants.ForestConstants.CONFIG_DATA
import com.empire.forest.data.ForestUserDatabase
import com.empire.forest.data.ForestUserDatabaseProvider
import com.empire.forest.gate.EscapeGateDescription
import com.empire.forest.generator.GeneratorDescription
import com.empire.forest.generic.TeamDamageNullifyFacet
import com.empire.forest.lobby.LobbyListener
import com.empire.forest.lobby.QueueSign
import com.empire.forest.mechanic.phantom.CloakingPerk
import com.empire.forest.mechanic.werewolf.WerewolfSpeedPerk
import com.empire.forest.perk.*
import com.empire.forest.tablist.ForestTablist
import com.empire.forest.util.ForestMatchStartEvent
import com.empire.ignite.Ignite
import com.empire.ignite.game.application.*
import com.empire.ignite.game.application.component.DeathTrackerContextComponent
import com.empire.ignite.game.application.component.IDeathTrackerComponent
import com.empire.ignite.game.application.component.IPlayerAccessComponent
import com.empire.ignite.game.application.component.PlayerAccessContextComponent
import com.empire.ignite.game.facets.*
import com.empire.ignite.team.IgniteTeam
import com.empire.ignite.team.IgniteTeamOptions
import com.empire.ignite.util.*
import com.empire.ignite.util.callback.ForwardingCallback
import com.empire.ignite.util.config.InteractiveConfigV2
import com.empire.ignite.util.item.ItemBuilder
import com.empire.ignite.util.location.RawLocation
import com.empire.ignite.util.region.CuboidRegion
import com.empire.ignite.util.region.Region
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team
import org.bukkit.util.Vector
import java.io.File
import java.util.*

class ForestApplication : IgniteApplicationV2<ForestStaticData, ForestContext>() {
    companion object {
        private val LOBBY_WORLD_NAME = "world"
        private val SPAWN_REGION = CuboidRegion(
            Vector(592, 22, 343),
            Vector(329,-17,127)
        )
        private val SPAWN_LOCATION = RawLocation(48.0,71.0,9.0)
        private val GAME_NAME = "The Forest"
    }
    private val appLevelDump = GlobalResourceTrackers.createResourceDump()
    private var userDatabase: ForestUserDatabase? = null

    override fun load(plugin: Ignite) {
        val lobbyWorld = Bukkit.getWorld(LOBBY_WORLD_NAME)!!
        val sign = QueueSign(plugin, Location(lobbyWorld, 50.0, 72.0, 11.0))
        sign.load()
        appLevelDump.add(sign)
        val lobbyListener = LobbyListener(plugin, SPAWN_LOCATION.toLocationNonNull(lobbyWorld), SPAWN_REGION)
        lobbyListener.load()
        appLevelDump.add(lobbyListener)

        userDatabase = ForestUserDatabaseProvider.create(
            File(getDirectory(plugin), "forest-data.db").absolutePath
        )
        appLevelDump.add(userDatabase!!)
    }

    override fun unload() {
        appLevelDump.destroyAll()
    }


    override fun getDisplayName(): Component =
        Component.text("The Forest").color(ForestConstants.FOREST_COLOR)

    override fun getName(): String = GAME_NAME

    override fun getDirectory(plugin: Ignite) =
        File(plugin.applicationsDirectory, "the_forest")

    override fun getCommands(plugin: Ignite): List<BaseCommand> {
        return listOf(
            QueueCommand(this, plugin),
            LeaveCommand(),
            SpectateCommand(plugin),
            TestIcvCommand(plugin),
            CoinsCommand(userDatabase!!)
        )
    }

    private fun interactiveData() : ForestInteractiveData? {
        val data = CONFIG_DATA.read() ?: return null
        val debug = ((data as InteractiveConfigV2.NamedStructure).entries
            .getOrDefault("isDebug", InteractiveConfigV2.BooleanData(false))
            as InteractiveConfigV2.BooleanData).b
        return ForestInteractiveData(debug)
    }

    override fun produceMatchData(
        appManager: IgniteApplicationManager,
        staticData: Any,
        id: UUID,
        workspace: File,
        plugin: Ignite
    ): Pair<ForestContext, ExecutionNodeProducer> {
        val forestData = staticData as ForestStaticData
        val dump = GlobalResourceTrackers.createResourceDump()
        val context = setupContext(plugin, forestData, dump)
        val definition = {
            FacetTimelines.sync {
                parent {
                    FacetTimelines.sync {
                        inline {
                            ForestMatchStartEvent(context).callEvent()
                        }
                        // queue!
                        node {
                            ApplicationHelpers.produceQueueTimeline(
                                plugin,
                                context,
                                this@ForestApplication,
                                queueSubscriber = object : QueueSubscriber {
                                    override fun onQueue(player: Player) {
                                        healStabilizePlayers(listOf(player))
                                        player.gameMode = GameMode.ADVENTURE
                                    }

                                    override fun onDequeue(player: Player) {}
                                },
                                options = QueueOptions(countdownLength = 20L * staticData.queueCountdownSeconds),
                                messagingTargets = { Bukkit.getOnlinePlayers().toSet() }
                            )
                        }

                        // load the world
                        node {
                            WorldLoaderFacet(
                                plugin, context,
                                File(staticData.worldPath),
                                workspace
                            )
                        }

                        // teams!
                        inline {
                            val survivorsTeam = IgniteTeam(
                                "Survivors",
                                emptySet(),
                                IgniteTeamOptions(
                                    displayName = Component.text("Survivors").color(
                                        ForestConstants.SURVIVORS_COLOR
                                    ),
                                    color = NamedTextColor.GOLD,
                                    friendlyFire = false,
                                    prefix = Component.text("").color(ForestConstants.SURVIVORS_COLOR),
                                    options = listOf(
                                        Team.Option.COLLISION_RULE to Team.OptionStatus.NEVER,
                                        // "for other teams" means hide for other teams... thanks Bukkit
                                        Team.Option.NAME_TAG_VISIBILITY to Team.OptionStatus.FOR_OTHER_TEAMS
                                    )
                                )
                            )
                            val huntersTeam = IgniteTeam(
                                "Hunters",
                                emptySet(),
                                IgniteTeamOptions(
                                    displayName = Component.text("Hunters").color(
                                        ForestConstants.HUNTERS_COLOR
                                    ),
                                    color = NamedTextColor.RED,
                                    friendlyFire = false,
                                    prefix = Component.text("").color(NamedTextColor.RED),
                                    options = listOf(
                                        Team.Option.COLLISION_RULE to Team.OptionStatus.NEVER,
                                        // "for other teams" means hide for other teams... thanks Bukkit
                                        Team.Option.NAME_TAG_VISIBILITY to Team.OptionStatus.FOR_OTHER_TEAMS
                                    )
                                )
                            )
                            context.teams += listOf(survivorsTeam, huntersTeam)
                            context.hunterTeam = huntersTeam
                            context.survivorTeam = survivorsTeam
                            context.setupChangeSetContracts()
                        }
                    }
                }.child {
                    // make sure no one kills each other or gets hungry during the setup phase
                    EnvironmentControlFacet(plugin, context, EnvironmentControlOptions(
                        damageHandler = { true }, // cancel dmg
                        hungerHandler = { true } // cancel hunger
                    ))
                }

                node {
                    FacetTimelines.async {
                        node {
                            DeathFacet(plugin, context, voidYLevel = staticData.yLevelDeath)
                        }
                        node {
                            SpectatorFacet(
                                plugin, context,
                                staticData.spectatorsSpawn.toLocation(context.world)!!
                            )
                        }
                    }
                }

                parent {
                    ForestFacet(plugin, context, dump)
                }.child {
                    FacetTimelines.async {
                        node {
                            TeamDamageNullifyFacet(plugin, context)
                        }
                    }
                }
//                node {
//                    ForestFacet(plugin, context, dump)
//                }
            }
        }
        return context to definition
    }

    private fun setupContext(plugin: Ignite, forestData: ForestStaticData, dump: ManagedResourceDump) : ForestContext {
        val context = ForestContext(
            this, forestData, interactiveData(),
            SPAWN_LOCATION.toLocationNonNull(Bukkit.getWorld(LOBBY_WORLD_NAME)!!),
            plugin
        )
        context.perks.addAll(
            listOf(
                BearTrapPerk(plugin, context),
                HeartbeatPerk(plugin, context),
                MotionSensorPerk(plugin, context),
                CloakingPerk(plugin),
                WerewolfSpeedPerk(plugin),
                RoarPerk(plugin, context),
                BuckshotPerk(plugin, context),
                NewMotionSensorPerk(plugin, context)
            )
        )
        dump.add(itemBuilderActiveListener(plugin, context.locateGeneratorsItemBuilder)  { p, b ->
            context.locateGeneratorsCallbackRegistration.sink(p to b)
        })
        dump.add(object : UnloadableResource {
            override fun unload(external: Boolean) {
                context.perks.forEach { it.unload(external) }
            }
        })
        return context
    }
}

typealias GeneratorSelection = ((List<GeneratorDescription>) -> (List<GeneratorDescription>))

class ForestStaticData(
    val mapName: String? = null,
    val minRequiredGenerators : Int = -1,
    val generatorSelection: GeneratorSelection? = null,
    val queueCountdownSeconds: Int,
    val survivorReleaseSeconds: Int,
    val hunterReleaseSeconds: Int,
    val yLevelDeath: Int,
    val worldPath: String,
    val survivorsSpawn: RawLocation,
    val survivorsRegion: CuboidRegion? = null,
    val huntersSpawn: RawLocation,
    val spectatorsSpawn: RawLocation,
    val generators: List<GeneratorDescription>,
    val escape: EscapeGateDescription,
    val survivorSpawnBarrierRegion: Region,
    val hunterSpawnBarrierRegion: Region,
    val contributionMultiplier: (Int) -> Double
)
class ForestContext(
    application: IgniteApplicationV2<ForestStaticData, *>,
    staticData: ForestStaticData, val interactiveData: ForestInteractiveData?,
    private val safeEvictLocation: Location,
    val plugin: Ignite
) : IgniteExecutionContext<ForestStaticData>(application, staticData),
    IMatchStateComponent, ITeamComponent, IWorldComponent, IDeathTrackerComponent, IPlayerAccessComponent {
    override var matchState: MatchState = MatchState.IDLE
    override val teams: MutableList<IgniteTeam> = mutableListOf()
    override var world: World? = null
    override val deathTracker: DeathTrackerContextComponent = DeathTrackerContextComponent()
    override val playerAccess: PlayerAccessContextComponent = PlayerAccessContextComponent(playerTracker, deathTracker)

    val locateGeneratorsItemBuilder = ItemBuilder(Material.RECOVERY_COMPASS) {
        name(Component.text("Locate Generators").color(NamedTextColor.GOLD))
        lore(
            InventoryUtils.postprocessLore(
                listOf(
                    Component.text("Hold this to see generators").color(NamedTextColor.YELLOW),
                )
            )
        )
        unbreakable()
    }
    val locateGeneratorsCallbackRegistration = ForwardingCallback.create<Pair<Player, Boolean>>()

    lateinit var hunterTeam : IgniteTeam
    lateinit var survivorTeam : IgniteTeam
    lateinit var survivorsSpawn: Location
    var survivorsRegion: CuboidRegion? = null
    lateinit var huntersSpawn: Location
    var tablist: ForestTablist? = null

    lateinit var survivorTeamChangeSetContract : ChangeSetContract<Player>
    lateinit var hunterTeamChangeSetContract : ChangeSetContract<Player>

    val perks : MutableList<ForestPerk<*>> = mutableListOf()

    fun loadSpawns() : Boolean {
        this.survivorsSpawn = staticData.survivorsSpawn.toLocation(world)!!
        this.survivorsRegion = staticData.survivorsRegion
        this.huntersSpawn = staticData.huntersSpawn.toLocation(world)!!
        return true
    }

    fun setupChangeSetContracts() {
        survivorTeamChangeSetContract = ChangingSetUtils.createChangingSet(survivorTeam.players)
        hunterTeamChangeSetContract = ChangingSetUtils.createChangingSet(hunterTeam.players)
    }

    fun <C> getPerk(perkClass: Class<out ForestPerk<C>>) : ForestPerk<C>? =
        perks.firstOrNull { perkClass.isAssignableFrom(it.javaClass) } as ForestPerk<C>?

    // utility functions
    fun sendToHunters(component: Component) {
        sendMessageToPlayers(hunterTeam.players, component)
    }

    fun sendToSurvivors(component: Component) {
        sendMessageToPlayers(hunterTeam.players, component)
    }

    fun sendToAll(component: Component) {
        sendMessageToPlayers(playerAccess.all, component)
    }

    fun isSurvivor(player: Player) = survivorTeam.players.contains(player)
    fun isHunter(player: Player) = hunterTeam.players.contains(player)

    fun canDamage(p1: Player, p2: Player) : Boolean {
        if (p1 !in playerAccess.alive || p2 !in playerAccess.alive) return false
        val p1team = findPlayerTeam(p1) ?: return false
        val p2team = findPlayerTeam(p2) ?: return false
        return p1team != p2team
    }

    fun addSurvivor(player: Player) {
        survivorTeam.addPlayer(player)
        survivorTeamChangeSetContract.addSink(player)
    }

    fun addHunter(player: Player) {
        hunterTeam.addPlayer(player)
        hunterTeamChangeSetContract.addSink(player)
    }

    fun removeFromTeam(player: Player) {
        if (isSurvivor(player)) {
            survivorTeam.removePlayer(player)
            survivorTeamChangeSetContract.removeSink(player)
        } else if (isHunter((player))) {
            hunterTeam.removePlayer(player)
            hunterTeamChangeSetContract.removeSink(player)
        }
    }

    override fun getWorldUnloadDecision(): WorldUnloadDecision {
        return WorldUnloadDecision.createSafeEvictThenAllow(safeEvictLocation)
    }

    fun getSurvivorChangeSet() : ChangingSet<Player> {
        return survivorTeamChangeSetContract.changeSet
    }

    fun getHunterChangeSet() : ChangingSet<Player> {
        return hunterTeamChangeSetContract.changeSet
    }
}