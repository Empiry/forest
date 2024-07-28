package com.empire.forest

import co.aikar.commands.BaseCommand
import com.empire.forest.command.QueueCommand
import com.empire.ignite.Ignite
import com.empire.ignite.game.application.*
import com.empire.ignite.game.facets.EnvironmentControlFacet
import com.empire.ignite.game.facets.EnvironmentControlOptions
import com.empire.ignite.game.facets.QueueSubscriber
import com.empire.ignite.team.IgniteTeam
import com.empire.ignite.team.IgniteTeamOptions
import com.empire.ignite.util.IGNITE_LOGGER
import com.empire.ignite.util.location.RawLocation
import com.empire.ignite.util.preparePlayers
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team
import java.io.File
import java.util.*

class ForestApplication : IgniteApplicationV2<ForestStaticData, ForestContext>() {
    override fun getDisplayName(): Component =
        Component.text("The Forest").color(TextColor.color(44, 145, 71))

    override fun getName(): String = "The Forest"

    override fun getCommands(plugin: Ignite): List<BaseCommand> {
        return listOf(QueueCommand(this, plugin))
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
                                options = QueueOptions(countdownLength = 20L * 8L)
                            )
                        }

                        // teams!
                        inline {
                            val survivorsTeam = IgniteTeam(
                                "Survivors",
                                emptySet(),
                                IgniteTeamOptions(
                                    displayName = Component.text("Survivors").color(NamedTextColor.GRAY),
                                    color = NamedTextColor.GRAY,
                                    friendlyFire = false,
                                    prefix = Component.text("").color(NamedTextColor.GRAY),
                                    options = listOf(
                                        Team.Option.COLLISION_RULE to Team.OptionStatus.NEVER
                                    )
                                )
                            )
                            val huntersTeam = IgniteTeam(
                                "Hunters",
                                emptySet(),
                                IgniteTeamOptions(
                                    displayName = Component.text("Infected").color(NamedTextColor.RED),
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
                    ForestFacet(plugin, context)
                }
            }
        }
        return context to definition
    }
}

class ForestStaticData(
    val worldName: String,
    val survivorsSpawn: RawLocation,
    val huntersSpawn: RawLocation
)
class ForestContext(
    application: IgniteApplicationV2<ForestStaticData, *>,
    staticData: ForestStaticData,
) : IgniteExecutionContext<ForestStaticData>(application, staticData),
    IMatchStateComponent, ITeamComponent {
    override var matchState: MatchState = MatchState.IDLE
    override val teams: MutableList<IgniteTeam> = mutableListOf()

    lateinit var hunterTeam : IgniteTeam
    lateinit var survivorTeam : IgniteTeam
    lateinit var survivorsSpawn: Location
    lateinit var huntersSpawn: Location

    fun loadSpawns() : Boolean {
        val world = Bukkit.getWorld(staticData.worldName) ?: return false
        this.survivorsSpawn = staticData.survivorsSpawn.toLocation(world)!!
        this.huntersSpawn = staticData.huntersSpawn.toLocation(world)!!
        return true
    }
}