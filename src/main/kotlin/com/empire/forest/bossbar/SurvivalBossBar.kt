package com.empire.forest.bossbar

import com.empire.forest.ForestContext
import com.empire.forest.perk.ComponentShape
import com.empire.ignite.Ignite
import com.empire.ignite.util.ChangingSet
import com.empire.ignite.util.IgniteResource
import com.empire.ignite.util.text.TextUtils.flattenComponents
import com.empire.ignite.util.ui.ManagedBossBar
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

class SurvivalBossBar(
    plugin: Ignite,
    private val context: ForestContext,
    private val totalTicks: Long,
    private val playerChangeSet: ChangingSet<Player>,
) : IgniteResource {
    private val bar: ManagedBossBar = ManagedBossBar(plugin.adventure())
    private var remaining : Long = totalTicks

    override fun load() {
        bar.setupPlayerChangeSet(playerChangeSet)
        render()
    }

    fun setTimeRemaining(remaining: Long) {
        this.remaining = remaining
        render()
    }

    fun render() {
        val ratio = remaining.toFloat() / totalTicks
        bar.bossbar.name(
            context.jamIfNeeded(
                flattenComponents(
                    Component.text("Time: ").color(NamedTextColor.AQUA),
                    formatTime(remaining)
                ),
                ComponentShape(25, NamedTextColor.AQUA)
            )
        )
        bar.bossbar.progress(ratio)
        bar.bossbar.color(BossBar.Color.BLUE)
    }

    override fun unload(external: Boolean) {
        bar.removePlayerChangeSet(playerChangeSet)
        bar.unload()
    }

    private fun formatTime(ticks: Long) : Component {
        val minutes = ((ticks * 50) / 1000) / 60
        val seconds = ((ticks * 50) / 1000) % 60
        val timeComponent = Component.text(
            String.format("%02d:%02d", minutes, seconds)
        ).color(NamedTextColor.AQUA)
        return timeComponent
    }
}