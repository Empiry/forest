package com.empire.forest.scoreboard

import com.empire.forest.ForestContext
import com.empire.forest.constants.ForestConstants
import com.empire.forest.generator.ForestGenerator
import com.empire.forest.generator.GeneratorDescription
import com.empire.forest.util.ForestMessaging
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
    val generators: List<Pair<GeneratorDescription, ForestGenerator>>
) : SillyScoreboardAgent {}

object SurvivorScoreboard {
    val GENERATOR_STATUSES_PUSH_KEY = "generator_statuses"
    val SURVIVOR_TEAM_COUNT_UPDATE = "survivor_team_count_update"
    val HUNTER_TEAM_COUNT_UPDATE = "hunter_team_count_update"

    fun create(
        plugin: JavaPlugin, context: ForestContext,
        generators: List<Pair<GeneratorDescription, ForestGenerator>>,
        changeSetSurvivors: ChangingSet<Player>,
        generatorProgressCallback: ForwardCallbackRegistration<Unit>,
        dump: ManagedResourceDump
    ) {
        val sb = SillyScoreboard(plugin, { player -> SurvivorScoreboardAgent(player, context, generators) }) {
            title { Component.text("The Forest").color(ForestConstants.SURVIVORS_COLOR) }
            numbering(30)

            lines {
                line {
                    Component.text(" ".repeat(16))
                }
                varyLine(pushKey = GENERATOR_STATUSES_PUSH_KEY) { agent ->
                    agent.generators.map { (desc, generator) ->
                        val progress = floor(
                            100.0F * (generator.progressTicks.toFloat() / (desc.unlockSeconds * 20))
                        ).toInt()
                        Component.text(desc.name).color(
                            ForestMessaging.getGeneratorProgressColor(progress)
                        ).append(
                            Component.text(" ${progress}%").color(NamedTextColor.YELLOW)
                        )
                    }
                }
                line {
                    Component.text(" ".repeat(16))
                }
                line(pushKey = SURVIVOR_TEAM_COUNT_UPDATE) {
                    Component.text("Survivors: ").color(ForestConstants.SURVIVORS_COLOR).append(
                        Component.text(context.survivorTeam.players.size).color(NamedTextColor.GREEN)
                    )
                }
                line(pushKey = HUNTER_TEAM_COUNT_UPDATE) {
                    Component.text("Hunters: ").color(NamedTextColor.RED).append(
                        Component.text(context.hunterTeam.players.size).color(NamedTextColor.GREEN)
                    )
                }
                line {
                    Component.text(" ".repeat(16))
                }
            }
        }
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
        generatorProgressCallback.register(
            PassthroughFilterCallback.createActiveUsingTicks(plugin, 7L, dump) {
                sb.trigger(SurvivorScoreboard.GENERATOR_STATUSES_PUSH_KEY)
            }
        )
    }
}