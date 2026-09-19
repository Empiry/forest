package com.empire.forest

import co.aikar.commands.BaseCommand
import com.empire.forest.bossbar.GameBossBar
import com.empire.forest.command.*
import com.empire.forest.config.ACTIVE_SERVER_CONFIG
import com.empire.forest.config.ForestInteractiveData
import com.empire.forest.constants.ForestConstants
import com.empire.forest.constants.ForestConstants.CONFIG_DATA
import com.empire.forest.constants.ForestConstants.GENERIC_QUEUE_END_TAG
import com.empire.forest.data.ForestUserData
import com.empire.forest.data.ForestUserDatabase
import com.empire.forest.data.ForestUserDatabaseProvider
import com.empire.forest.gate.EscapeGateDescription
import com.empire.forest.generator.ForestGeneratorNameplate
import com.empire.forest.generator.GeneratorDescription
import com.empire.forest.generator.NameplateProximityTask
import com.empire.forest.generic.TeamDamageNullifyFacet
import com.empire.forest.hud.QueueNametagIndicator
import com.empire.forest.lobby.LobbyListener
import com.empire.forest.lobby.QueueSign
import com.empire.forest.mechanic.SurvivorHealth
import com.empire.forest.mechanic.stamina.SurvivorStamina
import com.empire.forest.perk.ComponentShape
import com.empire.forest.perk.ForestPerk
import com.empire.forest.perk.JammerUtils
import com.empire.forest.perk.StaredownPerk
import com.empire.forest.scoreboard.SurvivorScoreboardAgent
import com.empire.forest.tablist.ForestTablist
import com.empire.forest.util.ForestMatchStartEvent
import com.empire.ignite.Ignite
import com.empire.ignite.game.application.*
import com.empire.ignite.game.application.component.DeathTrackerContextComponent
import com.empire.ignite.game.application.component.IDeathTrackerComponent
import com.empire.ignite.game.application.component.IPlayerAccessComponent
import com.empire.ignite.game.application.component.PlayerAccessContextComponent
import com.empire.ignite.game.facets.*
import com.empire.ignite.match.event.MatchGenericEvent
import com.empire.ignite.team.IgniteTeam
import com.empire.ignite.team.IgniteTeamOptions
import com.empire.ignite.util.*
import com.empire.ignite.util.callback.ForwardingCallback
import com.empire.ignite.util.collection.OnlinePlayerData
import com.empire.ignite.util.config.InteractiveConfigV2
import com.empire.ignite.util.item.ItemBuilder
import com.empire.ignite.util.location.RawLocation
import com.empire.ignite.util.region.CuboidRegion
import com.empire.ignite.util.region.Region
import com.empire.ignite.util.scoreboard.SillyScoreboard
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scoreboard.Team
import java.io.File
import java.util.*
import java.util.concurrent.Future

class ForestApplication : IgniteApplicationV2<ForestStaticData, ForestContext>() {
    companion object {
        private val GAME_NAME = "Hunting Grounds"
    }
    private val appLevelDump = GlobalResourceTrackers.createResourceDump()
    private var userDatabase: ForestUserDatabase? = null

    override fun load(plugin: Ignite) {
        val lobbyWorld = Bukkit.getWorld(ACTIVE_SERVER_CONFIG.lobby.lobbyWorld)!!
        val sign = QueueSign(
            plugin,
            Location(
                lobbyWorld,
                ACTIVE_SERVER_CONFIG.lobby.queueSignLoc.x,
                ACTIVE_SERVER_CONFIG.lobby.queueSignLoc.y,
                ACTIVE_SERVER_CONFIG.lobby.queueSignLoc.z
            )
        )
        sign.load()
        appLevelDump.add(sign)
        val lobbyListener = LobbyListener(
            plugin,
            ACTIVE_SERVER_CONFIG.lobby.spawnLocation.toLocationNonNull(lobbyWorld),
            ACTIVE_SERVER_CONFIG.lobby.spawnRegion
        )
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
        Component.text("Hunting Grounds").color(ForestConstants.FOREST_COLOR)

    override fun getName(): String = GAME_NAME

    override fun getDirectory(plugin: Ignite) =
        File(plugin.applicationsDirectory, "the_forest")

    override fun getCommands(plugin: Ignite): List<BaseCommand> {
        return listOf(
            QueueCommand(this, plugin),
            LeaveCommand(),
            SpectateCommand(plugin),
            TestIcvCommand(plugin),
            CoinsCommand(userDatabase!!),
            ForestDebugCommand()
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
                            val countdownTicks = 20L * staticData.queueCountdownSeconds
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
                                options = QueueOptions(
                                    countdownLength = countdownTicks,
                                    chatMessageFormatter = ForestConstants.getQueueMessageFormatter(
                                        countdownTicks
                                    ),
                                    additionalSubscribers = listOf(QueueNametagIndicator(plugin))
                                ),
                                messagingTargets = { Bukkit.getOnlinePlayers().toSet() }
                            )
                        }

                        // async fetch user match prefs
                        inline {
                            context.playerDataFetchFuture = userDatabase!!.getDataBatch(
                                context.playerAccess.all.map { it.uniqueId }
                            )
                        }

                        // load the world
                        node {
                            WorldLoaderFacet(
                                plugin, context,
                                File(staticData.worldPath),
                                workspace,
                                spawnLocation = staticData.spectatorsSpawn
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
                            MatchGenericEvent(id, tag = GENERIC_QUEUE_END_TAG).callEvent()
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
                    ForestFacet(plugin, context, userDatabase!!, dump)
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
            ACTIVE_SERVER_CONFIG.lobby.spawnLocation.toLocationNonNull(
                Bukkit.getWorld(ACTIVE_SERVER_CONFIG.lobby.lobbyWorld)!!
            ),
            plugin
        )
//        val machetePerk = MachetePerk(plugin, context)
//        context.messageBus.add(machetePerk)
//        context.perks.addAll(
//            listOf(
//                BearTrapPerk(plugin, context),
//                HeartbeatPerk(plugin, context),
//                CloakingPerk(plugin),
//                WerewolfSpeedPerk(plugin),
//                RoarPerk(plugin, context),
//                BuckshotPerk(plugin, context),
//                NewMotionSensorPerk(plugin, context),
//                BloodhoundPerk(plugin, context),
//                JammerPerk(plugin, context),
//                machetePerk,
//                StaredownPerk(plugin, context),
//                ShadowPerk(plugin, context)
//            )
//        )
//        dump.add(object : UnloadableResource {
//            override fun unload(external: Boolean) {
//                context.perks.forEach { it.unload(external) }
//            }
//        })
        dump.add(itemBuilderActiveListener(plugin, context.locateGeneratorsItemBuilder)  { p, b ->
            context.proximityTask?.overrides[p] = b
            context.locateGeneratorsCallbackRegistration.sink(p to b)
        })
        return context
    }
}

typealias GeneratorSelection = ((List<GeneratorDescription>) -> (List<GeneratorDescription>))

class ForestStaticData(
    val mapName: String? = null,
    val minRequiredGeneratorsFn : (ForestContext) -> Int = {-1},
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
    val contributionMultiplier: (Int) -> Double,
    val overwriteRegions: List<Region> = listOf()
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
    lateinit var survivorHealth: SurvivorHealth
    var stamina: SurvivorStamina? = null
    var survivorsRegion: CuboidRegion? = null
    lateinit var huntersSpawn: Location
    var tablist: ForestTablist? = null
    val nameplates : MutableList<ForestGeneratorNameplate> = mutableListOf()
    var proximityTask : NameplateProximityTask? = null
    var jammed: Boolean = false
    var scoreboard: SillyScoreboard<SurvivorScoreboardAgent>? = null
    var gameBossBar: GameBossBar? = null
    val forestPlayerContextData : OnlinePlayerData<ForestContextualPlayerData> = OnlinePlayerData(
        plugin,
        onEvict = {_, u -> u.unload() }
    )
    var playerDataFetchFuture : Future<Map<UUID, ForestUserData>>? = null

    lateinit var survivorTeamChangeSetContract : ChangeSetContract<Player>
    lateinit var hunterTeamChangeSetContract : ChangeSetContract<Player>
    lateinit var spectatorsChangeSetContract : ChangeSetContract<Player>

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
        spectatorsChangeSetContract = ChangingSetUtils.createChangingSet(playerAccess.spectating)
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

    fun getDisplayName(player: Player) : Component {
        val color = if (isHunter(player)) ForestConstants.HUNTERS_COLOR
        else if (isSurvivor(player)) ForestConstants.SURVIVORS_COLOR
        else ForestConstants.SURVIVORS_COLOR
        return Component.text(player.name).color(color)
    }

    fun addSpectator(player: Player) : Boolean {
        return playerTracker.addSpectator(player)
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

    fun getForestPlayerContextData(player: Player) : ForestContextualPlayerData? {
        if (player !in playerAccess.all) return null
        if (player !in forestPlayerContextData) {
            forestPlayerContextData[player] = ForestContextualPlayerData(player)
        }
        return forestPlayerContextData[player]
    }

    fun gameProbablyOver() : Boolean {
        return hunterTeam.players.isEmpty() || survivorTeam.players.isEmpty()
    }

    fun jamIfNeeded(component: Component, shape: ComponentShape) : Component =
        if (jammed) JammerUtils.jam(shape)
        else component

    override fun getWorldUnloadDecision(): WorldUnloadDecision {
        return WorldUnloadDecision.createSafeEvictThenAllow(safeEvictLocation)
    }

    fun getSurvivorChangeSet() : ChangingSet<Player> {
        return survivorTeamChangeSetContract.changeSet
    }

    fun getHunterChangeSet() : ChangingSet<Player> {
        return hunterTeamChangeSetContract.changeSet
    }

    fun getSpectatorsChangeSet() : ChangingSet<Player> {
        return spectatorsChangeSetContract.changeSet
    }

    fun getNormalMovementSpeed(player: Player) : Double {
        val base = 0.10000000149011612
        val forestPerk = getPerk(StaredownPerk::class.java) ?: return base
        val staredown = forestPerk as StaredownPerk
        return if (staredown.using(player)) base * 1.25 else base
    }

    fun shouldReplenishHunger(player: Player): Boolean {
        if (player !in playerAccess.alive) return false
        if (player in survivorTeam.players) return false
        val forestPerk = getPerk(StaredownPerk::class.java) ?: return true
        val staredown = forestPerk as StaredownPerk
        return !staredown.isSeen(player)
    }
}

class ForestContextualPlayerData(
    private val player: Player
) : UnloadableResource {
    private var arrowReplenishmentTaskSetup: Boolean = false
    private val dump = GlobalResourceTrackers.createResourceDump()

    fun setupArrowReplenishmentTask() {
        if (arrowReplenishmentTaskSetup) return
        dump.add(GlobalResourceTrackers.scheduler.repeatTask(2L) {
            if (player.inventory.getItem(17)?.type != Material.ARROW) {
                player.inventory.setItem(17, ItemStack(Material.ARROW))
            }
        })
    }

    override fun unload(external: Boolean) {
        dump.destroyAll()
    }
}