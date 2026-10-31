package com.empire.forest.lobby

import com.empire.forest.ForestContext
import com.empire.forest.config.ServerConfig
import com.empire.ignite.Ignite
import com.empire.ignite.game.application.UnspectateMatchEvent
import com.empire.ignite.game.facets.system.PlayerRemoveReason
import com.empire.ignite.match.event.PlayerQuitMatchEvent
import com.empire.ignite.util.*
import com.empire.ignite.util.item.ItemBuilder
import com.empire.ignite.util.region.Region
import com.empire.ignite.util.region.RegionPlayerTracker
import com.empire.ignite.util.region.RegionPlayerTrackerCallback
import com.empire.ignite.util.scoreboard.SillyScoreboard
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.block.BlockFace
import org.bukkit.block.data.type.Tripwire
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPhysicsEvent
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
import org.bukkit.util.Vector

class LobbyListener(
    private val plugin: Ignite,
    private val config: ServerConfig,
    private val spawnLocation: Location,
    private val spawnEncompassingRegion: Region
) : Listener, IgniteResource {

    private val lobbyScoreboard : SillyScoreboard<LobbyScoreboardAgent> = getLobbyScoreboard(plugin)
    private lateinit var spawnRegionPlayerTracker: RegionPlayerTracker
    private lateinit var lobbyQueueItem: ItemBuilder
    private lateinit var queueItemListener: UnloadableResource
    private val DO_NOT_MESS_WITH_ME : (Player) -> Boolean = { player ->
        if (config.prod) false
        else player.gameMode == GameMode.CREATIVE || player.isOp
    }

    private val DO_NOT_MESS_WITH_ME_NO_PROD_CHECK : (Player) -> Boolean = { player ->
        player.gameMode == GameMode.CREATIVE || player.isOp
    }

    init {
        for (player in Bukkit.getOnlinePlayers()) {
            lobbyScoreboard.safeBind(player)
        }
    }

    @EventHandler
    private fun onPlayerJoin(event: PlayerJoinEvent) {
        if (DO_NOT_MESS_WITH_ME(event.player)) return
        lobbyScoreboard.safeBind(event.player)
        event.player.getAttribute(Attribute.SCALE)?.baseValue = 1.2
        event.player.teleport(spawnLocation)
    }

    @EventHandler
    private fun onStringPhysicsChange(event: BlockPhysicsEvent) {
        if (event.block.type != Material.TRIPWIRE) return
        event.isCancelled = true
        val block = event.block
        val blockData = block.blockData
        if (blockData is Tripwire) {
            blockData.setFace(BlockFace.NORTH, false)
            blockData.setFace(BlockFace.SOUTH, false)
            blockData.setFace(BlockFace.EAST, false)
            blockData.setFace(BlockFace.WEST, false)
            blockData.isAttached = false
            blockData.isDisarmed = false
            blockData.isPowered = false
            block.setBlockData(blockData, false)
            block.getState().update(true, false)
        }
    }

    @EventHandler
    private fun onPlayerQuitMatch(event: PlayerQuitMatchEvent) {
        if (event.match.context !is ForestContext) return
        healAndClearInventoryOfPlayers(listOf(event.player))
        event.player.teleport(spawnLocation)
        GlobalResourceTrackers.scheduler.after(2L) {
            lobbyScoreboard.safeBind(event.player)
        }
    }

    @EventHandler
    private fun onPlayerUnspectate(event: UnspectateMatchEvent) {
        if (event.match.context !is ForestContext) return
        if (event.reason != PlayerRemoveReason.BY_WILL) return
        healAndClearInventoryOfPlayers(listOf(event.player))
        event.player.teleport(spawnLocation)
        GlobalResourceTrackers.scheduler.after(2L) {
            lobbyScoreboard.safeBind(event.player)
        }
    }

    @EventHandler
    private fun onDamage(event: EntityDamageEvent) {
        if (event.entity !is Player) return
        if (event.entity.world != spawnLocation.world) return
        if (event.entity.location.toVector() !in spawnEncompassingRegion) return
        event.isCancelled = true
    }

    @EventHandler
    private fun onBlockBreak(event: BlockBreakEvent) {
        if (DO_NOT_MESS_WITH_ME_NO_PROD_CHECK(event.player)) return
        if (event.player.world != spawnLocation.world) return
        if (event.player.location.toVector() !in spawnEncompassingRegion) return
        event.isCancelled = true
    }

    @EventHandler
    private fun onBlockPlace(event: BlockPlaceEvent) {
        if (DO_NOT_MESS_WITH_ME_NO_PROD_CHECK(event.player)) return
        if (event.player.world != spawnLocation.world) return
        if (event.player.location.toVector() !in spawnEncompassingRegion) return
        event.isCancelled = true
    }

    @EventHandler
    private fun onItemDrop(event: PlayerDropItemEvent) {
        if (DO_NOT_MESS_WITH_ME(event.player)) return
        if (event.player.world != spawnLocation.world) return
        if (event.player.location.toVector() !in spawnEncompassingRegion) return
        event.isCancelled = true
    }

    @EventHandler
    private fun onInventoryOpen(event: InventoryOpenEvent) {
        if (event.player !is Player) return
        if (DO_NOT_MESS_WITH_ME(event.player as Player)) return
        if (event.player.world != spawnLocation.world) return
        if (event.player.location.toVector() !in spawnEncompassingRegion) return
        if (event.inventory.type == InventoryType.PLAYER || event.inventory.type == InventoryType.CREATIVE) return
        event.isCancelled = true
    }

    @EventHandler
    private fun onInventoryManipulate(event: InventoryClickEvent) {
        if (event.whoClicked !is Player) return
        if (DO_NOT_MESS_WITH_ME(event.whoClicked as Player)) return
        if (event.whoClicked.world != spawnLocation.world) return
        if (event.whoClicked.location.toVector() !in spawnEncompassingRegion) return
        if (event.inventory.type != InventoryType.PLAYER) return
        event.isCancelled = true
    }

    @EventHandler
    private fun onItemFrameChange(event: PlayerItemFrameChangeEvent) {
        if (DO_NOT_MESS_WITH_ME_NO_PROD_CHECK(event.player)) return
        if (event.player.world != spawnLocation.world) return
        if (event.player.location.toVector() !in spawnEncompassingRegion) return
        event.isCancelled = true
    }

    @EventHandler
    private fun onHunger(event: FoodLevelChangeEvent) {
        if (event.entity !is Player) return
        val player = event.entity as Player
        if (player.world != spawnLocation.world) return
        if (player.location.toVector() !in spawnEncompassingRegion) return
        event.isCancelled = true
        player.foodLevel = 20
    }

    private fun reset(player: Player) {
        if (DO_NOT_MESS_WITH_ME(player)) return
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
            if (player.world != spawnLocation.world) return@ugh
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