package com.empire.forest.perk

import com.empire.forest.ForestContext
import com.empire.forest.constants.ForestConstants
import com.empire.forest.mechanic.CooldownRightClickAbility
import com.empire.forest.mechanic.CooldownRightClickAbilityOptions
import com.empire.forest.util.ForestUtils
import com.empire.forest.util.PlayerDataAccess
import com.empire.ignite.util.item.ItemBuilder
import com.empire.ignite.util.playSoundToPlayers
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class RoarPerk(
    plugin: JavaPlugin,
    private val context: ForestContext
) : ForestPerk<Unit> {
    private val ability: CooldownRightClickAbility
    private val item: ItemStack
    override val contextManager: PlayerDataAccess<Unit> = PlayerDataAccess.noop()

    init {
        val itemBuilder = ItemBuilder(Material.BELL) {
            name(Component.text("RAWR XD").color(ForestConstants.HUNTERS_COLOR))
            lore(
                listOf(
                    Component.text("Right-click to YELL VIOLENTLY")
                        .color(NamedTextColor.RED)
                )
            )
        }
        ability = CooldownRightClickAbility(
            plugin, 20L * 45L,
            CooldownRightClickAbilityOptions(consume = false),
            itemBuilder,
            ::action
        )
        item = itemBuilder.build()
        item.amount = 1
    }

    override fun apply(player: Player) {
        ability.add(player)
        player.give(item)
    }

    private fun action(player: Player) {
        playSoundToPlayers(
            context.playerAccess.all,
            Sound.ENTITY_WARDEN_ROAR,
            2.0F,
            0.0F
        )
        ForestUtils.fakeGlow(
            listOf(player),
            context.survivorTeam.players.toList(),
            7L * 20L
        )
    }

    override fun remove(player: Player) {
        ability.remove(player)
    }

    override fun unload(external: Boolean) {
        ability.unload(external)
    }
}