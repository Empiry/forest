package com.empire.forest.generator

import com.empire.forest.ForestContext
import com.empire.ignite.Ignite
import com.empire.ignite.util.IgniteResource
import com.empire.ignite.util.text.TextUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.Listener

class ForestGenerator(
    private val ignite: Ignite,
    private val forestContext: ForestContext,
    private val location: Location,
    unlockSeconds: Int,
    private val onComplete: () -> Unit
) : IgniteResource, Listener {
    var completed : Boolean = false
        private set
    private var tickerTask : Int = -1
    private val unlockTicks = unlockSeconds*20
    private var progress : Int = 0

    override fun load() {
        tickerTask = Bukkit.getScheduler().runTaskTimer(ignite, this::process, 0L, 1L).taskId
    }

    private val workingPlayers : MutableList<Player> = mutableListOf()
    private fun process() {
        if (completed) return
        var activeContributors = 0
        for (player in forestContext.playerTracker.players) {
            if (player.location.distanceSquared(this.location) <= 4) {
                workingPlayers += player
                if (player.isSneaking) activeContributors++
            }
        }
        progress += activeContributors
        val completion = ((progress.toDouble() / (unlockTicks)) * 100).toInt()
        val message = TextUtils.createProgressText("|", completion, 15)
        workingPlayers.forEach { workingPlayer ->
            if (workingPlayer.isSneaking) workingPlayer.sendActionBar(message)
            else
                workingPlayer.sendActionBar(
                    Component.text(
                        "Sneak to activate generator...", NamedTextColor.GRAY, TextDecoration.ITALIC
                    )
                )
        }
        workingPlayers.clear()
        if (progress >= unlockTicks) {
            completed = true
            if (tickerTask != -1) Bukkit.getScheduler().cancelTask(tickerTask)
            onComplete()
        }
    }

    override fun unload(external: Boolean) {
        if (tickerTask != -1) Bukkit.getScheduler().cancelTask(tickerTask)
    }
}