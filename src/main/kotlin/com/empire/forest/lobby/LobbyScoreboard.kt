package com.empire.forest.lobby

import com.empire.forest.ForestContext
import com.empire.forest.constants.ForestConstants
import com.empire.ignite.Ignite
import com.empire.ignite.game.application.MatchState
import com.empire.ignite.match.MatchV2
import com.empire.ignite.util.scoreboard.SillyScoreboard
import com.empire.ignite.util.scoreboard.SillyScoreboardAgent
import com.empire.ignite.util.text.TextUtils.flattenComponents
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

fun getLobbyScoreboard(plugin: Ignite) : SillyScoreboard<LobbyScoreboardAgent> =
    SillyScoreboard(plugin, { LobbyScoreboardAgent(plugin)}) {
        title { Component.text("Hunting Grounds").color(ForestConstants.FOREST_COLOR) }
        numbering(30)

        lines {
            line {
                Component.text(" ".repeat(16))
            }

            varyLine {
                agent -> agent.getMatchStatus()
            }

            line {
                Component.text(" ".repeat(16))
            }

            line {
                Component.text("play.huntinggrounds.party").color(NamedTextColor.YELLOW)
            }
        }
    }

class LobbyScoreboardAgent(
    private val plugin: Ignite
) : SillyScoreboardAgent {
    fun getMatchStatus() : List<Component> {
        val matches = plugin.matchManager.matches.filter { match ->
            match.context is ForestContext
        }
        return if (matches.isEmpty()) {
            listOf(
                Component.text("No matches ongoing").color(NamedTextColor.RED),
                flattenComponents(
                    Component.text("/q").color(NamedTextColor.YELLOW),
                    Component.text(" to start one!").color(NamedTextColor.GRAY),
                )
            )
        } else if (matches.size == 1) {
            getIndividualMatchStatus(matches[0].context as ForestContext)
        } else {
            listOf(Component.text("${matches.size} matches ongoing").color(NamedTextColor.GREEN))
        }
    }

    private fun getIndividualMatchStatus(ctx: ForestContext) : List<Component> {
        return when (ctx.matchState) {
            MatchState.IDLE -> listOf(
                flattenComponents(
                    Component.text("Looking for players ").color(NamedTextColor.GRAY),
                    Component.text("(/q)").color(NamedTextColor.YELLOW),
                )
            )
            MatchState.QUEUE -> {
                listOf(
                    Component.text("${ctx.playerTracker.players.size} queued ").color(NamedTextColor.GREEN),
                    flattenComponents(
                        Component.text("/q").color(NamedTextColor.YELLOW),
                        Component.text(" to join").color(NamedTextColor.GRAY),
                    )
                )
            }
            MatchState.READY -> listOf(Component.text("Match in progress").color(NamedTextColor.GRAY))
            MatchState.PANIC -> listOf(Component.text("Unknown issue").color(NamedTextColor.RED))
        }
    }
}