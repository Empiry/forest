package com.empire.forest.lobby

import com.empire.ignite.util.*
import com.empire.ignite.util.item.ItemBuilder
import com.empire.ignite.util.region.Region
import com.empire.ignite.util.region.RegionPlayerTracker
import com.empire.ignite.util.region.RegionPlayerTrackerCallback
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector

class LobbyListener(
    private val plugin: JavaPlugin,
    private val spawnLocation: Location,
    private val spawnEncompassingRegion: Region
) : Listener, IgniteResource {
    companion object {
        private val DO_NOT_MESS_WITH_ME : (Player) -> Boolean = { player ->
            player.gameMode == GameMode.CREATIVE || player.isOp
        }
    }

    private lateinit var spawnRegionPlayerTracker: RegionPlayerTracker
    private lateinit var lobbyQueueItem: ItemBuilder
    private lateinit var queueItemListener: UnloadableResource

    @EventHandler
    private fun onPlayerJoin(event: PlayerJoinEvent) {
        if (DO_NOT_MESS_WITH_ME(event.player)) return
        event.player.teleport(spawnLocation)
    }

    @EventHandler
    private fun onDamage(event: EntityDamageEvent) {
        if (event.entity !is Player) return
        if (event.entity.location.toVector() !in spawnEncompassingRegion) return
        event.isCancelled = true
    }

    @EventHandler
    private fun onBlockBreak(event: BlockBreakEvent) {
        if (DO_NOT_MESS_WITH_ME(event.player)) return
        if (event.player.location.toVector() !in spawnEncompassingRegion) return
        event.isCancelled = true
    }

    @EventHandler
    private fun onBlockPlace(event: BlockPlaceEvent) {
        if (DO_NOT_MESS_WITH_ME(event.player)) return
        if (event.player.location.toVector() !in spawnEncompassingRegion) return
        event.isCancelled = true
    }

    @EventHandler
    private fun onItemDrop(event: PlayerDropItemEvent) {
        if (DO_NOT_MESS_WITH_ME(event.player)) return
        if (event.player.location.toVector() !in spawnEncompassingRegion) return
        event.isCancelled = true
    }

    @EventHandler
    private fun onInventoryOpen(event: InventoryOpenEvent) {
        if (event.player !is Player) return
        if (DO_NOT_MESS_WITH_ME(event.player as Player)) return
        if (event.player.location.toVector() !in spawnEncompassingRegion) return
        if (event.inventory.type == InventoryType.PLAYER || event.inventory.type == InventoryType.CREATIVE) return
        event.isCancelled = true
    }

    @EventHandler
    private fun onInventoryManipulate(event: InventoryClickEvent) {
        if (event.whoClicked !is Player) return
        if (DO_NOT_MESS_WITH_ME(event.whoClicked as Player)) return
        if (event.whoClicked.location.toVector() !in spawnEncompassingRegion) return
        if (event.inventory.type != InventoryType.PLAYER) return
        event.isCancelled = true
    }

    @EventHandler
    private fun onItemFrameChange(event: PlayerItemFrameChangeEvent) {
        if (DO_NOT_MESS_WITH_ME(event.player)) return
        if (event.player.location.toVector() !in spawnEncompassingRegion) return
        event.isCancelled = true
    }

    @EventHandler
    private fun onHunger(event: FoodLevelChangeEvent) {
        if (event.entity !is Player) return
        val player = event.entity as Player
        if (DO_NOT_MESS_WITH_ME(player)) return
        if (player.location.toVector() !in spawnEncompassingRegion) return
        event.isCancelled = true
        player.foodLevel = 20
    }

    private fun reset(player: Player) {
        player.gameMode = GameMode.ADVENTURE
        player.health = player.getAttribute(Attribute.MAX_HEALTH)!!.value
        player.inventory.clear()
        player.velocity = Vector(0, 0, 0)
        for (potionEffect in player.activePotionEffects) {
            player.removePotionEffect(potionEffect.type)
        }
        player.foodLevel = 20
        player.fireTicks = 0
        for (resourceType in PlayerExclusiveResourceType.entries) {
            GlobalResourceTrackers.getPlayerResources(player).exclusiveResources.evictResourceType(resourceType)
        }
        player.inventory.addItem(this.lobbyQueueItem.build())
    }

    override fun load() {
        registerListener(plugin, this)
        this.spawnRegionPlayerTracker = RegionPlayerTracker(plugin, spawnEncompassingRegion)
        this.spawnRegionPlayerTracker.load()
        this.spawnRegionPlayerTracker.callbacks += object : RegionPlayerTrackerCallback {
            override fun onPlayerEnter(movementEvent: Cancellable, player: Player) {
                reset(player)
            }

            override fun onPlayerExit(movementEvent: Cancellable, player: Player) {}
        }
        this.lobbyQueueItem = ItemBuilder(Material.CLOCK) {
            name(Component.text("Queue (Right Click)").color(NamedTextColor.GREEN))
            flags(setOf(ItemFlag.HIDE_ATTRIBUTES))
        }
        this.queueItemListener = itemBuilderRightClickListener(
            plugin, this.lobbyQueueItem
        ) ugh@{ player: Player, item: ItemStack ->
            if (player.location.toVector() !in this.spawnEncompassingRegion) return@ugh
            player.performCommand("queue")
        }
    }

    override fun unload(external: Boolean) {
        unregisterListener(this)
        this.spawnRegionPlayerTracker.unload()
        this.queueItemListener.unload()
    }
}