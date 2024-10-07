package com.empire.forest.bossbar

import com.empire.forest.ForestContext
import com.empire.ignite.Ignite
import com.empire.ignite.util.ChangingSet
import com.empire.ignite.util.IgniteResource
import com.empire.ignite.util.ui.ManagedBossBar
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

class GameBossBar(
    plugin: Ignite,
    private val context: ForestContext,
    private val generatorCount: Int,
    private val playerChangeSet: ChangingSet<Player>
) : IgniteResource {
    private val bar: ManagedBossBar = ManagedBossBar(plugin.adventure())
    private var generatorsComplete = 0

    override fun load() {
        bar.setupPlayerChangeSet(playerChangeSet)
        render()
    }

    fun setGeneratorsComplete(completed: Int) {
        generatorsComplete = completed
        render()
    }

    private fun render() {
        if (generatorsComplete < generatorCount) {
            val ratio = generatorsComplete.toFloat() / generatorCount
            bar.bossbar.name(
                Component.text("${generatorsComplete}/${generatorCount} generators complete")
                    .color(NamedTextColor.YELLOW)
            )
            bar.bossbar.progress(ratio)
            bar.bossbar.color(BossBar.Color.PURPLE)
        } else {
            bar.bossbar.name(
                Component.text("All generators complete!").color(NamedTextColor.GREEN)
                    .append(Component.text(" Survivors can escape!").color(NamedTextColor.YELLOW))
            )
            bar.bossbar.progress(1.0F)
            bar.bossbar.color(BossBar.Color.GREEN)
        }
    }

    override fun unload(external: Boolean) {
        bar.removePlayerChangeSet(playerChangeSet)
        bar.unload()
    }
}