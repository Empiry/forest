package com.empire.forest.scoreboard

import com.empire.forest.ForestContext
import com.empire.forest.constants.ForestConstants
import com.empire.forest.generator.ForestGenerator
import com.empire.forest.generator.ForestGeneratorDiscovery
import com.empire.forest.generator.GeneratorDescription
import com.empire.forest.perk.ComponentShape
import com.empire.forest.util.ForestMessaging
import com.empire.forest.util.ForestMessaging.GENERATOR_NOT_STARTED_COLOR
import com.empire.ignite.util.*
import com.empire.ignite.util.callback.ForwardCallbackRegistration
import com.empire.ignite.util.callback.PassthroughFilterCallback
import com.empire.ignite.util.scoreboard.SillyScoreboard
import com.empire.ignite.util.scoreboard.SillyScoreboardAgent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import kotlin.math.floor

class SurvivorScoreboardAgent(
    private val player: Player,
    private val context: ForestContext,
    val generators: List<Pair<GeneratorDescription, ForestGenerator>>,
    val discovery: ForestGeneratorDiscovery
) : SillyScoreboardAgent {}

object SurvivorScoreboard {
    val GENERATOR_STATUSES_PUSH_KEY = "generator_statuses"
    val SURVIVOR_TEAM_COUNT_UPDATE = "survivor_team_count_update"
    val HUNTER_TEAM_COUNT_UPDATE = "hunter_team_count_update"

    fun create(
        plugin: JavaPlugin, context: ForestContext,
        generators: List<Pair<GeneratorDescription, ForestGenerator>>,
        discovery: ForestGeneratorDiscovery,
        changeSetSurvivors: ChangingSet<Player>,
        generatorProgressCallback: ForwardCallbackRegistration<Unit>,
        dump: ManagedResourceDump
    ) {
        val sb = SillyScoreboard(
            plugin, {
                player -> SurvivorScoreboardAgent(player, context, generators, discovery)
            }
        ) {
            title { Component.text("Hunting Grounds").color(ForestConstants.FOREST_COLOR) }
            numbering(30)

            lines {
                line {
                    Component.text(" ".repeat(16))
                }
                varyLine(pushKey = GENERATOR_STATUSES_PUSH_KEY) { agent ->
                    agent.generators.map { (desc, generator) ->
                        if (!agent.discovery.unlocked(desc)) {
                            return@map context.jamIfNeeded(
                                Component.text("${desc.name} ???").color(
                                    GENERATOR_NOT_STARTED_COLOR
                                ),
                                ComponentShape(desc.name.length + 4, GENERATOR_NOT_STARTED_COLOR)
                            )
                        }
                        val progress = floor(
                            100.0F * (generator.progressTicks.toFloat() / (desc.unlockSeconds * 20))
                        ).toInt()
                        val color = ForestMessaging.getGeneratorProgressColor(progress)
                        context.jamIfNeeded(
                            Component.text(desc.name).color(
                                color
                            ).append(
                                Component.text(" ${progress}%").color(NamedTextColor.YELLOW)
                            ),
                            ComponentShape(desc.name.length, color)
                        )
                    }
                }
                line {
                    Component.text(" ".repeat(16))
                }
                line(pushKey = SURVIVOR_TEAM_COUNT_UPDATE) {
                    val c = Component.text("Survivors: ").color(ForestConstants.SURVIVORS_COLOR).append(
                        Component.text(context.survivorTeam.players.size).color(NamedTextColor.GREEN)
                    )
                    context.jamIfNeeded(
                        c,
                        ComponentShape(15, ForestConstants.SURVIVORS_COLOR)
                    )
                }
                line(pushKey = HUNTER_TEAM_COUNT_UPDATE) {
                    context.jamIfNeeded(
                        Component.text("Hunters: ").color(ForestConstants.HUNTERS_COLOR).append(
                            Component.text(context.hunterTeam.players.size).color(NamedTextColor.GREEN)
                        ),
                        ComponentShape(15, ForestConstants.HUNTERS_COLOR)
                    )
                }
                line {
                    Component.text(" ".repeat(16))
                }
            }
        }
        context.scoreboard = sb
        val csr = object : ChangingSetReceiver<Player> {
            override fun onAdd(item: Player) {
                sb.trigger(SURVIVOR_TEAM_COUNT_UPDATE)
                sb.trigger(HUNTER_TEAM_COUNT_UPDATE)
            }

            override fun onRemove(item: Player) {
                sb.trigger(SURVIVOR_TEAM_COUNT_UPDATE)
                sb.trigger(HUNTER_TEAM_COUNT_UPDATE)
            }
        }
        changeSetSurvivors.setupOnAdd(csr)
        changeSetSurvivors.setupOnRemove(csr)
        val unloader = object : UnloadableResource {
            override fun unload(external: Boolean) {
                changeSetSurvivors.removeOnAdd(csr)
                changeSetSurvivors.removeOnRemove(csr)
            }
        }
        dump.add(sb)
        dump.add(unloader)
        dump.add(
            ChangingSetUtils.bindToScoreboard(context.getSurvivorChangeSet(), sb)
        )
        dump.add(
            ChangingSetUtils.bindToScoreboard(context.getSpectatorsChangeSet(), sb)
        )
        generatorProgressCallback.register(
            PassthroughFilterCallback.createActiveUsingTicks(plugin, 7L, dump) {
                sb.trigger(SurvivorScoreboard.GENERATOR_STATUSES_PUSH_KEY)
            }
        )
        discovery.onDiscover.register { sb.trigger(SurvivorScoreboard.GENERATOR_STATUSES_PUSH_KEY) }
    }
}