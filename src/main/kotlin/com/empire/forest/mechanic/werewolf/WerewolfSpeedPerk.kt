package com.empire.forest.mechanic.werewolf

import com.empire.forest.mechanic.CooldownRightClickAbility
import com.empire.forest.mechanic.CooldownRightClickAbilityOptions
import com.empire.forest.perk.ForestPerk
import com.empire.forest.util.PlayerDataAccess
import com.empire.ignite.util.item.ItemBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class WerewolfSpeedPerk(
    plugin: JavaPlugin
) : ForestPerk<Unit> {
    private val ability: CooldownRightClickAbility
    private val item: ItemStack
    override val contextManager: PlayerDataAccess<Unit> = PlayerDataAccess.noop()

    init {
        val itemBuilder = ItemBuilder(Material.QUARTZ) {
            name(Component.text("Speed Boost").color(NamedTextColor.BLUE))
            lore(
                listOf(
                        Component.text("Right-click to gain a 4 second speed-boost")
                            .color(NamedTextColor.AQUA)
                )
            )
        }
        ability = CooldownRightClickAbility(
            plugin, 20L * 15L, CooldownRightClickAbilityOptions(), itemBuilder,
            ::action
        )
        item = itemBuilder.build()
        item.amount = 3
    }

    override fun apply(player: Player) {
        ability.add(player)
        player.give(item)
    }

    private fun action(player: Player) {
        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.SPEED, 20 * 4, 1,
                true, false
            )
        )
    }

    override fun remove(player: Player) {
        ability.remove(player)
    }

    override fun unload(external: Boolean) {
        ability.unload(external)
    }
}