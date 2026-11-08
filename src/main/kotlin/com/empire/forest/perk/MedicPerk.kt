package com.empire.forest.perk

import com.empire.forest.ForestContext
import com.empire.forest.util.PlayerDataAccess
import com.empire.ignite.util.collection.OnlinePlayerSet
import com.empire.ignite.util.item.ItemBuilder
import com.empire.ignite.util.itemCompare
import com.empire.ignite.util.registerListener
import com.empire.ignite.util.removeItemOfCompare
import com.empire.ignite.util.unregisterListener
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Consumable
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation
import io.papermc.paper.event.player.PlayerArmSwingEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import kotlin.math.min

class MedicPerk(
    plugin: JavaPlugin,
    private val context: ForestContext
) : ForestPerk<Unit>, Listener {
    override val contextManager: PlayerDataAccess<Unit> = PlayerDataAccess.noop()
    private val itemBuilder = ItemBuilder(Material.STICK) {
        itemModel(NamespacedKey("horror", "utility/bottle_of_pills"))
        name(Component.text("Bottle of Pills").color(NamedTextColor.GREEN))
        unbreakable()
        dataComponent(DataComponentTypes.CONSUMABLE, Consumable.consumable()
            .consumeSeconds(3.0f).animation(ItemUseAnimation.DRINK).build())
    }
    private val compare : (ItemStack) -> Boolean
    private val data : OnlinePlayerSet = OnlinePlayerSet()

    init {
        compare = itemCompare(plugin, itemBuilder)
        data.load()
        registerListener(plugin, this)
    }

    override fun apply(player: Player) {
        data += player
        val i = itemBuilder.build()
        i.amount = 2
        player.give(i)
    }

    @EventHandler
    private fun onItemConsume(event: PlayerItemConsumeEvent) {
        val player = event.player
        if (player !in data) return
        if (!compare(event.item)) return
        if (player !in context.survivorTeam.players) return
        val health = context.survivorHealth.getHealth(player) ?: return
        health.health = min(100, health.health + 35)
    }

    @EventHandler
    private fun onArmSwing(event: PlayerArmSwingEvent) {
        if (event.player !in data) return
        if (!compare(event.player.inventory.itemInMainHand)) return
        val hit = event.player.rayTraceEntities(3, false)?.hitEntity ?: return
        if (hit !is Player || hit !in context.survivorTeam.players) return
        val health = context.survivorHealth.getHealth(hit) ?: return
        health.health = min(100, health.health + 35)
        removeItemOfCompare(event.player, compare, 1)
    }

    override fun remove(player: Player) {
        data -= player
    }

    override fun unload(external: Boolean) {
        data.unload(external)
        unregisterListener(this)
    }
}