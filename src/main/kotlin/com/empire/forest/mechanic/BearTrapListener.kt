package com.empire.forest.mechanic

import com.empire.forest.ForestContext
import com.empire.ignite.util.*
import com.empire.ignite.util.item.ItemBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.*
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

class BearTrapListener(
    private val plugin: JavaPlugin,
    private val context: ForestContext,
    private val dump: ManagedResourceDump
) : Listener, IgniteResource {
    private val keyName = "forestbeartrap"
    private val key = NamespacedKey(plugin, keyName)
    val bearTrapItemFn : (Player) -> ItemBuilder = {
        ItemBuilder(Material.SHEARS) {
            name(Component.text("Bear Trap!").color(NamedTextColor.YELLOW))

            lore(InventoryUtils.postprocessLore(listOf(
                Component.text("Drop this to plant a bear trap!").color(NamedTextColor.RED)
            )))

            unbreakable()
            pdcString(key, it.uniqueId.toString())
        }
    }

    private var taskId = -1
    override fun load() {
        registerListener(plugin, this)
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, ::process, 0L, 4L)
    }

    private fun process() {
        val items = context.world?.getEntitiesByClasses(Item::class.java) ?: return
        items.forEach { entity ->
            if (!entity.isOnGround) return@forEach
            if (entity !is Item) return@forEach
            val dropper = entity.itemStack.persistentDataContainer.get(key, PersistentDataType.STRING)
                ?: return@forEach
            val planter = Bukkit.getPlayer(dropper)
            entity.world.playSound(entity.location.clone(), Sound.UI_BUTTON_CLICK, 1.0F, 1.0F)
            val location = entity.location.clone()
            entity.remove()
            spawnBearTrap(planter, location)
        }
    }

    private fun spawnBearTrap(planter: Entity?, location: Location) {
        val bt = BearTrap(plugin, context, planter, location)
        bt.load()
        dump.add(bt)
    }

    override fun unload(external: Boolean) {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId)
            taskId = -1
        }
        unregisterListener(this)
    }
}