package com.empire.forest.perk

import com.empire.forest.ForestContext
import com.empire.forest.mechanic.CooldownRightClickAbility
import com.empire.forest.mechanic.CooldownRightClickAbilityOptions
import com.empire.forest.util.PlayerDataAccess
import com.empire.ignite.util.InventoryUtils
import com.empire.ignite.util.item.ItemBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class FearPerk(
    plugin: JavaPlugin,
    private val context: ForestContext
) : ForestPerk<Unit> {
    override val contextManager: PlayerDataAccess<Unit> = PlayerDataAccess.noop()
    private val itemBuilder = ItemBuilder(Material.STICK) {
        name(Component.text("Fear").color(NamedTextColor.DARK_GRAY))
        itemModel(NamespacedKey("horror", "ability/fear"))
        lore(
            InventoryUtils.postprocessLore(
                listOf(Component.text("This SPOOKS the survivors"))
            )
        )
        unbreakable()
    }
    val cooldownAbility : CooldownRightClickAbility
    private val item : ItemStack

    init {
        cooldownAbility = CooldownRightClickAbility(
            plugin, 20L * 60L,
            CooldownRightClickAbilityOptions(consume = false), itemBuilder,
            this::use
        )
        item = itemBuilder.build()
    }

    override fun apply(player: Player) {
        cooldownAbility.add(player)
        player.give(item)
    }

    private fun use(player: Player) {
        context.survivorTeam.players.forEach { survivor ->
            survivor.addPotionEffect(
                PotionEffect(
                    PotionEffectType.DARKNESS, 20 * 5, 0,
                    true, false
                )
            )
            context.stamina?.addStamina(survivor, -50.0)
        }
    }

    override fun remove(player: Player) {
        cooldownAbility.remove(player)
    }

    override fun unload(external: Boolean) {
        cooldownAbility.unload()
    }
}