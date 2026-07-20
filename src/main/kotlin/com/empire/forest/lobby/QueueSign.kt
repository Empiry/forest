package com.empire.forest.lobby

import com.empire.forest.ForestContext
import com.empire.forest.util.ForestMatchStartEvent
import com.empire.ignite.Ignite
import com.empire.ignite.game.application.MatchState
import com.empire.ignite.match.event.MatchEndEvent
import com.empire.ignite.util.IgniteResource
import com.empire.ignite.util.registerListener
import com.empire.ignite.util.unregisterListener
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Sign
import org.bukkit.block.sign.Side
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class QueueSign(
    private val plugin: Ignite,
    private val signLocation: Location
) : IgniteResource, Listener {
    private var activeMatchContext: ForestContext? = null
    private var updateTaskID = -1

    override fun load() {
        registerListener(plugin, this)
        updateTaskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, ::process, 0L, 2L)
    }

    @EventHandler
    private fun onMatchStart(event: ForestMatchStartEvent) {
        activeMatchContext = event.context
    }

    @EventHandler
    private fun onMatchEnd(event: MatchEndEvent) {
        if (event.match.context == activeMatchContext) {
            activeMatchContext = null
        }
    }

    private fun process() {
        val sign = signLocation.world.getBlockAt(signLocation)
        if (sign.state !is Sign) return
        val signState = sign.state as Sign
        signState.isWaxed = true
        val signSide = signState.getSide(Side.FRONT)
        val context = activeMatchContext
        if (context == null) {
            signSide.line(0, Component.empty())
            signSide.line(1, Component.text("Right click to ")
                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/q")))
            signSide.line(2, Component.text("Queue!"))
            signSide.line(3, Component.empty())
        } else {
            when (context.matchState) {
                MatchState.IDLE -> {
                    signSide.line(0, Component.empty())
                    signSide.line(1, Component.text("0").color(NamedTextColor.DARK_GREEN)
                        .append(Component.text(" queued").color(NamedTextColor.GREEN))
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/q"))
                    )
                    signSide.line(2, Component.text("Be the change").decorate(TextDecoration.ITALIC))
                    signSide.line(3, Component.text("you want to see").decorate(TextDecoration.ITALIC))
                }
                MatchState.QUEUE -> {
                    val queued = context.playerTracker.players.size
                    signSide.line(0, Component.empty())
                    signSide.line(1,
                        Component.text(
                            queued.toString()
                        ).color(NamedTextColor.DARK_GREEN)
                        .append(Component.text(
                            " queued!"
                        ).color(NamedTextColor.GREEN))
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/q"))
                    )
                    if (queued == 0) {
                        signSide.line(2, Component.text("Be the change").decorate(TextDecoration.ITALIC))
                        signSide.line(3, Component.text("you want to see").decorate(TextDecoration.ITALIC))
                    } else {
                        signSide.line(2, Component.empty())
                        signSide.line(3, Component.empty())
                    }
                }
                MatchState.READY -> {
                    signSide.line(0, Component.empty())
                    signSide.line(1, Component.text("Match started").color(NamedTextColor.RED))
                    signSide.line(2, Component.text("Spectate?").color(NamedTextColor.YELLOW)
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/spectate")))
                    signSide.line(3, Component.empty())
                }
                MatchState.PANIC -> {
                    signSide.line(0, Component.empty())
                    signSide.line(1, Component.text("Error"))
                    signSide.line(2, Component.empty())
                    signSide.line(3, Component.empty())
                }
            }
        }
        signState.update()
    }

    override fun unload(external: Boolean) {
        unregisterListener(this)
        if (updateTaskID != -1) {
            Bukkit.getScheduler().cancelTask(updateTaskID)
            updateTaskID = -1
        }
    }
}