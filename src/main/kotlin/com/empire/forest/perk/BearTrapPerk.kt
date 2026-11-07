package com.empire.forest.perk

import com.empire.forest.ForestContext
import com.empire.forest.mechanic.BearTrap
import com.empire.forest.util.BoundedLRUAccessor
import com.empire.forest.util.PlayerDataAccess
import com.empire.ignite.game.kit.ability.AbilityCooldown
import com.empire.ignite.game.kit.ability.AbilityUseListener
import com.empire.ignite.util.GlobalResourceTrackers
import com.empire.ignite.util.InventoryUtils
import com.empire.ignite.util.ManagedResourceDump
import com.empire.ignite.util.UnloadableResource
import com.empire.ignite.util.UnloadableResourcePostHook
import com.empire.ignite.util.collection.OnlinePlayerData
import com.empire.ignite.util.item.ItemBuilder
import com.empire.ignite.util.registerListener
import com.empire.ignite.util.timer.Timer
import com.empire.ignite.util.unregisterListener
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector
import java.util.UUID

class BearTrapPerk(
     val plugin: JavaPlugin,
    private val context: ForestContext
) : ForestPerk<BearTrapPlayerData>, Listener {
    private var scannerTaskID = -1
    private val keyName = "forestbeartrap"
    private val key = NamespacedKey(plugin, keyName)
    val bearTrapItemFn : (Player) -> ItemBuilder = {
        ItemBuilder(Material.STONE) {
            name(Component.text("Bear Trap!").color(NamedTextColor.YELLOW))
            itemModel(NamespacedKey("horror", "objects/survivor_beartrap"))

            lore(InventoryUtils.postprocessLore(listOf(
                Component.text("Drop this to plant a bear trap!").color(NamedTextColor.RED)
            )))

            unbreakable()
            pdcString(key, it.uniqueId.toString())
        }
    }
    val cooldown: AbilityCooldown = AbilityCooldown(
        plugin, 20L * 3L,
        abilityUseListeners = listOf(
            object : AbilityUseListener {
                override fun onAbilityCanUse(player: Player) {
                    bearTrapSelector.pokeItemCheck(player)
                }
            }
        )
    )
    override val contextManager: PlayerDataAccess<BearTrapPlayerData> = PlayerDataAccess.adaptPlayerData(
        OnlinePlayerData(plugin, onEvict = {_, trapData -> trapData.unload() })
    )
    private val dump: ManagedResourceDump = GlobalResourceTrackers.createResourceDump()
    private val bearTrapSelector: FakeBlockSelector = FakeBlockSelector(
        plugin,
        BlockSelectorOptions(
                selectorItemModel = NamespacedKey("horror", "objects/survivor_beartrap"),
                shadowItemModel = NamespacedKey("horror", "objects/survivor_beartrap_blueprint"),
                itemBuilderFn = {
                    name(Component.text("Bear Trap").color(NamedTextColor.GOLD))
                },
                offset = Vector(0.0, 0.5, 0.0),
        ),
        listener = object : FakeBlockSelector.BlockSelectorListener {
            override fun onTryUseItemActive(player: Player): Boolean {
                return this@BearTrapPerk.tryUse(player, verbose = false)
            }

            override fun onTryUseRightClick(player: Player): Boolean {
                return this@BearTrapPerk.tryUse(player, verbose = true)
            }

            override fun onBlockPlace(placeContext: FakeBlockSelector.PlaceContext) {
                onBearTrapPlace(placeContext)
            }

            override fun shouldSpawnItemDisplay(): Boolean = false
        }
    )

    init {
        registerListener(plugin, this)
//        scannerTaskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, ::process, 0L, 4L)
//        dump.add(GlobalResourceTrackers.scheduler.repeat(15L) {
//            contextManager.players().filter { player ->
//                player.inventory.storageContents.none { it != null && isBearTrap(it) }
//            }.forEach { player ->
//                giveBearTrap(player)
//            }
//        })
        bearTrapSelector.load()
        dump.add(bearTrapSelector)
        dump.add(object : UnloadableResource {
            override fun unload(external: Boolean) {
                cooldown.unload()
            }
        })
    }

    override fun apply(player: Player) {
        val data = contextManager.getData(player) ?: return
        bearTrapSelector.give(
            player,
            amount = if (data.consumeConfig is BearTrapConsumeConfig.Limited) data.consumeConfig.count else 1
        )
//        giveBearTrap(player)
    }

    private fun giveBearTrap(player: Player) {
        val itemBuilder = bearTrapItemFn(player)
        itemBuilder.amount(1)
        val trap = itemBuilder.build()
        player.inventory.addItem(trap)
    }

    override fun remove(player: Player) {
    }

    override fun unload(external: Boolean) {
        if (scannerTaskID != -1) {
            Bukkit.getScheduler().cancelTask(scannerTaskID)
            scannerTaskID = -1
        }
        contextManager.unload(external)
        dump.destroyAll()
        unregisterListener(this)
    }

    @EventHandler
    private fun onDrop(event: PlayerDropItemEvent) {
        if (event.player !in contextManager.players()) return
        if (!isBearTrap(event.itemDrop.itemStack)) return
        val data = contextManager.getData(event.player) ?: return
        if (data.didDrop) return
        if (!data.canUse) {
            event.isCancelled = true
        } else {
            data.canUse = false
            val timer = GlobalResourceTrackers.scheduler.afterUntracked(60L) {
                data.canUse = true
                data.activeTimer = null
            }
            data.activeTimer = timer
            dump.addSelfTracking(timer)
        }
    }

    private fun onBearTrapPlace(placeContext: FakeBlockSelector.PlaceContext) {
        val planter = placeContext.player
        val location = placeContext.location
        spawnBearTrap(planter, location)
    }


    private fun process() {
        val items = context.world?.getEntitiesByClasses(Item::class.java) ?: return
        items.forEach { entity ->
            if (!entity.isOnGround) return@forEach
            if (entity !is Item) return@forEach
            val dropper = entity.itemStack.persistentDataContainer.get(key, PersistentDataType.STRING)
                ?: return@forEach
            val planter = Bukkit.getPlayer(UUID.fromString(dropper))
            entity.world.playSound(entity.location.clone(), Sound.UI_BUTTON_CLICK, 1.0F, 1.0F)
            val location = entity.location.clone()
            entity.remove()
            spawnBearTrap(planter, location)
        }
    }

    private fun tryUse(player: Player, verbose: Boolean = false) : Boolean {
        if (player !in contextManager.players()) return false
        val result = cooldown.canUse(player)
        if (verbose) {
            cooldown.genericCooldownMessage(player)?.let {
                player.sendMessage(it)
            }
        }
        return result
    }

    private fun spawnBearTrap(planter: Player?, location: Location) {
        if (planter == null || !cooldown.use(planter)) return
        val isSurvivor = planter in context.survivorTeam.players
        contextManager.getData(planter)?.let {
            val bt = BearTrap(
                plugin, context, planter, location.clone().subtract(Vector(0.0, 0.5, 0.0)),
                if (isSurvivor) BearTrap.Type.SURVIVOR else BearTrap.Type.HUNTER
            )
            bt.load()
            it.recordBearTrap(bt)
            if (it.consumeConfig is BearTrapConsumeConfig.Limited) {
                bearTrapSelector.remove(planter)
            }
//            it.canUse = false
//            it.didDrop = false
//            val timer = GlobalResourceTrackers.scheduler.afterUntracked(100L) {
//                it.canUse = true
//                it.activeTimer = null
//            }
//            it.activeTimer = timer
//            dump.addSelfTracking(timer)
        }
    }

    private fun isBearTrap(istack: ItemStack) =
        istack.persistentDataContainer.get(key, PersistentDataType.STRING) != null
}

sealed class BearTrapConsumeConfig {
    class Limited(val count: Int) : BearTrapConsumeConfig()
    object Rolling : BearTrapConsumeConfig()
}

class BearTrapPlayerData(
    val trapCount: Int,
    var canUse: Boolean = true,
    var didDrop: Boolean = false,
    var activeTimer: Timer? = null,
    val consumeConfig : BearTrapConsumeConfig = BearTrapConsumeConfig.Rolling
) : UnloadableResource {
    val lru : BoundedLRUAccessor<BearTrap> = BoundedLRUAccessor(trapCount)

    fun recordBearTrap(bearTrap: BearTrap) {
        when (val result = lru.emplaceItem(bearTrap)) {
            is BoundedLRUAccessor.Result.Replaced<BearTrap> -> {
                result.item.unload()
            }
            is BoundedLRUAccessor.Result.Success<*> -> {}
        }
        bearTrap.onTrap {
            lru.removeItem(it)
        }
    }

    override fun unload(external: Boolean) {
        lru.buffer.forEach { it.unload(external) }
    }
}