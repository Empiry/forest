package com.empire.forest

import co.aikar.commands.BaseCommand
import com.empire.forest.command.LeaveCommand
import com.empire.forest.command.QueueCommand
import com.empire.forest.command.SpectateCommand
import com.empire.forest.command.TestIcvCommand
import com.empire.forest.constants.ForestConstants
import com.empire.forest.gate.EscapeGateDescription
import com.empire.forest.generator.GeneratorDescription
import com.empire.forest.mechanic.ForestMechanics
import com.empire.forest.tablist.ForestTablist
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
import com.empire.ignite.util.location.RawLocation
import com.empire.ignite.util.region.Region
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team
import java.io.File
import java.util.*

class ForestApplication : IgniteApplicationV2<ForestStaticData, ForestContext>() {
    private val WORLD_PATH = "./gamemaps/forestmap"

    override fun getDisplayName(): Component =
        Component.text("The Forest").color(ForestConstants.FOREST_COLOR)

    override fun getName(): String = "The Forest"

    override fun getCommands(plugin: Ignite): List<BaseCommand> {
        return listOf(
            QueueCommand(this, plugin),
            LeaveCommand(),
            SpectateCommand(),
            TestIcvCommand(plugin)
        )
    }

    override fun produceMatchData(
        appManager: IgniteApplicationManager,
        staticData: Any,
        id: UUID,
        workspace: File,
        plugin: Ignite
    ): Pair<ForestContext, ExecutionNodeProducer> {
        val forestData = staticData as ForestStaticData
        val context = ForestContext(this, forestData)
        val definition = {
            FacetTimelines.sync {
                parent {
                    FacetTimelines.sync {
                        // queue!
                        node {
                            ApplicationHelpers.produceQueueTimeline(
                                plugin,
                                context,
                                this@ForestApplication,
                                queueSubscriber = object : QueueSubscriber {
                                    override fun onQueue(player: Player) {
                                        preparePlayers(listOf(player))
                                    }

                                    override fun onDequeue(player: Player) {}
                                },
                                options = QueueOptions(countdownLength = 20L * 3L)
                            )
                        }

                        // load the world
                        node {
                            WorldLoaderFacet(
                                plugin, context,
                                File(WORLD_PATH),
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
                                        Team.Option.COLLISION_RULE to Team.OptionStatus.NEVER
                                    )
                                )
                            )
                            val huntersTeam = IgniteTeam(
                                "Hunters",
                                emptySet(),
                                IgniteTeamOptions(
                                    displayName = Component.text("Hunters").color(NamedTextColor.RED),
                                    color = NamedTextColor.RED,
                                    friendlyFire = false,
                                    prefix = Component.text("").color(NamedTextColor.RED),
                                    options = listOf(
                                        Team.Option.COLLISION_RULE to Team.OptionStatus.NEVER
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
                            DeathFacet(plugin, context, voidYLevel = -10)
                        }
                        node {
                            SpectatorFacet(
                                plugin, context, null
                            )
                        }
                    }
                }

                node {
                    ForestFacet(plugin, context)
                }
            }
        }
        return context to definition
    }
}

class ForestStaticData(
    val survivorsSpawn: RawLocation,
    val huntersSpawn: RawLocation,
    val generators: List<GeneratorDescription>,
    val escape: EscapeGateDescription,
    val survivorSpawnBarrierRegion: Region,
    val spectatorInitialLocation: RawLocation
)
class ForestContext(
    application: IgniteApplicationV2<ForestStaticData, *>,
    staticData: ForestStaticData
) : IgniteExecutionContext<ForestStaticData>(application, staticData),
    IMatchStateComponent, ITeamComponent, IWorldComponent, IDeathTrackerComponent, IPlayerAccessComponent {
    override var matchState: MatchState = MatchState.IDLE
    override val teams: MutableList<IgniteTeam> = mutableListOf()
    override var world: World? = null
    override val deathTracker: DeathTrackerContextComponent = DeathTrackerContextComponent()
    override val playerAccess: PlayerAccessContextComponent = PlayerAccessContextComponent(playerTracker, deathTracker)

    lateinit var hunterTeam : IgniteTeam
    lateinit var survivorTeam : IgniteTeam
    lateinit var survivorsSpawn: Location
    lateinit var huntersSpawn: Location
    lateinit var tablist: ForestTablist

    lateinit var survivorTeamChangeSetContract : ChangeSetContract<Player>
    lateinit var hunterTeamChangeSetContract : ChangeSetContract<Player>

    lateinit var forestMechanics : ForestMechanics

    fun loadSpawns() : Boolean {
        this.survivorsSpawn = staticData.survivorsSpawn.toLocation(world)!!
        this.huntersSpawn = staticData.huntersSpawn.toLocation(world)!!
        return true
    }

    fun setupChangeSetContracts() {
        survivorTeamChangeSetContract = ChangingSetUtils.createChangingSet(survivorTeam.players)
        hunterTeamChangeSetContract = ChangingSetUtils.createChangingSet(hunterTeam.players)
    }

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

    fun getSurvivorChangeSet() : ChangingSet<Player> {
        return survivorTeamChangeSetContract.changeSet
    }

    fun getHunterChangeSet() : ChangingSet<Player> {
        return hunterTeamChangeSetContract.changeSet
    }
}