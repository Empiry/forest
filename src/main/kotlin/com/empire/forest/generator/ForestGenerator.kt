package com.empire.forest.generator

import com.empire.forest.ForestContext
import com.empire.ignite.Ignite
import com.empire.ignite.util.IgniteResource
import com.empire.ignite.util.location.RawLocation
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
    private val onProgress: (Int) -> Unit,
    private val onComplete: () -> Unit
) : IgniteResource, Listener {
    var completed : Boolean = false
        private set
    private var tickerTask : Int = -1
    private val unlockTicks = unlockSeconds*20
    var progressTicks : Int = 0
        private set

    override fun load() {
        tickerTask = Bukkit.getScheduler().runTaskTimer(ignite, this::process, 0L, 1L).taskId
    }

    private val workingPlayers : MutableList<Player> = mutableListOf()
    private fun process() {
        if (completed) return
        var activeContributors = 0
        for (player in forestContext.playerTracker.players) {
            if (player.location.world != this.location.world) continue
            if (player.location.distanceSquared(this.location) <= 4) {
                workingPlayers += player
                if (player.isSneaking) {
                    if (forestContext.isSurvivor(player))
                        activeContributors++
                    else if (forestContext.isHunter(player))
                        activeContributors--
                }
            }
        }
        progressTicks += activeContributors
        progressTicks = progressTicks.coerceAtLeast(0)
        val completion = ((progressTicks.toDouble() / (unlockTicks)) * 100).toInt()
        if (activeContributors != 0) onProgress(completion)
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
        if (progressTicks >= unlockTicks) {
            completed = true
            if (tickerTask != -1) Bukkit.getScheduler().cancelTask(tickerTask)
            onComplete()
        }
    }

    override fun unload(external: Boolean) {
        if (tickerTask != -1) Bukkit.getScheduler().cancelTask(tickerTask)
    }
}

class GeneratorDescription(
    val name: String,
    val place: RawLocation,
    val unlockSeconds: Int
)