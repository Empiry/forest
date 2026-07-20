package com.empire.forest.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import com.empire.forest.ForestApplication
import com.empire.forest.ForestContext
import com.empire.forest.config.ForestStaticDataConfig
import com.empire.ignite.Ignite
import com.empire.ignite.match.MatchConfiguration
import com.empire.ignite.player.IgnitePlayerTracker
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

@CommandAlias("q|queue")
class QueueCommand(
    private val app: ForestApplication,
    private val plugin: Ignite
) : BaseCommand() {
    @Default
    fun createQueueMenu(player: Player) {
        if (IgnitePlayerTracker.isInMatch(player)) {
            player.sendMessage(
                Component.text(
                    "You are already in a match! If you would like to leave, /leave?",
                    NamedTextColor.RED
                )
            )
            return
        }
        val maxMatches = 1
        val forestMatches = plugin.matchManager.matches.filter {
            match -> match.context is ForestContext
        }
        val queueableMatch = run {
            val queueableMatch = forestMatches.find { forestMatch -> forestMatch.isAcceptingPlayers() }
            if (queueableMatch != null && queueableMatch.isAcceptingPlayers()) {
                queueableMatch
            } else {
                if (forestMatches.size >= maxMatches) {
                    null
                } else {
                    val newMatch = plugin.matchManager.scheduleMatch(
                        MatchConfiguration("main", app, ForestStaticDataConfig.MY_TEST_SERVER)
                    )
                    newMatch.start()
                    newMatch
                }
            }
        }
        if (queueableMatch == null) {
            player.sendMessage(
                Component.text("There are no queueable matches. Try /spectate to spectate!")
                    .color(NamedTextColor.RED)
            )
            return
        }
        queueableMatch.toggleQueuePlayer(player)
//        val matchesInProgress = forestMatches.filter { forestMatch ->
//            (forestMatch.context as ForestContext).matchState == MatchState.READY
//        }
//        val pageProvider = { facilities: GuiComposerFacilities ->
//            GuiSinglePageComponent(
//                facilities,
//                app.getDisplayName(),
//                3 * 9
//            ) {
//                slot(13, ItemBuilder(Material.FERN) {
//                    name(Component.text("Queue!").color(NamedTextColor.GREEN))
//                    lore(listOf(
//                        Component.text("Queue for a match!").color(NamedTextColor.GRAY)
//                    ))
//                }.build()) { ctx ->
//                    this.cancelClickEvent(true)
//                    val result = queueableMatch.toggleQueuePlayer(ctx.player)
//                    when (result) {
//                        MatchQueueResult.QUEUE_SUCCESS -> {
//                            player.sendMessage(
//                                Component.text("Queued!")
//                                    .color(NamedTextColor.GREEN)
//                            )
//                        }
//                        MatchQueueResult.QUEUE_FAILURE -> {
//                            player.sendMessage(
//                                Component.text("Could not queue, this match is likely full")
//                                    .color(NamedTextColor.RED)
//                            )
//                        }
//                        MatchQueueResult.UNQUEUE -> {
//                            player.sendMessage(
//                                Component.text("Removed you from the queue!")
//                                    .color(NamedTextColor.RED)
//                            )
//                        }
//                        MatchQueueResult.NOT_ACCEPTING_PLAYERS -> {
//                            player.sendMessage(
//                                Component.text("This match is not accepting players anymore")
//                                    .color(NamedTextColor.RED)
//                            )
//                        }
//                    }
//                    this.unbindPlayer(ctx.player)
//                }
//
//                var startSlot = 22 - (matchesInProgress.size / 2)
//                matchesInProgress.forEachIndexed { index, match ->
//                    slot(startSlot++, ItemBuilder(Material.ENDER_EYE) {
//                        name(Component.text("Spectate Match #${index + 1}").color(NamedTextColor.AQUA))
//                    }.build()) {
//                        this.cancelClickEvent(true)
//                    }
//                }
//           }
//        }
//        val gui = GuiComposer(plugin)
//        gui.addComponentProducer(pageProvider)
//        gui.show(player)
    }
}