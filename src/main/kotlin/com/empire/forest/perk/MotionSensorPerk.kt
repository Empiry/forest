package com.empire.forest.perk

import com.empire.forest.ForestContext
import com.empire.forest.util.ForestUtils
import com.empire.forest.util.PlayerDataAccess
import com.empire.hacks.common.builder.ClientEntityMetadataHelpers
import com.empire.ignite.util.*
import com.empire.ignite.util.collection.OnlinePlayerData
import com.empire.ignite.util.entity.ClientEntity
import com.empire.ignite.util.item.ItemBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameEvent
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.LightningStrike
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockReceiveGameEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector

class MotionSensorPerk(
    plugin: JavaPlugin,
    private val context: ForestContext
) : ForestPerk<Unit>, Listener {
    private val bs = BlockSelector(plugin, ::onBlockPlace)
    private val sensorBlocks : MutableSet<Block> = mutableSetOf()

    companion object {
        private val SENSOR_BLOCK_DATA = Material.CALIBRATED_SCULK_SENSOR.createBlockData()
    }

    init {
        bs.load()
        registerListener(plugin, this)
    }

    private fun onBlockPlace(block: Block) {
        sensorBlocks.add(block)
    }

    @EventHandler
    private fun onSculk(event: BlockReceiveGameEvent) {
        if (event.block !in sensorBlocks) return
        if (event.event != GameEvent.STEP) {
            event.isCancelled = true
            return
        }
        if (event.entity in context.survivorTeam.players) {
            val survivor = event.entity as Player
            survivor.location.world.spawn(event.block.location, LightningStrike::class.java)
            context.playerAccess.all.forEach {
                it.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.NIGHT_VISION, 60, 1, true, false
                    )
                )
            }
            ForestUtils.fakeGlow(
                context.hunterTeam.players.toList() + survivor, listOf(survivor),
                15L * 20L
            )
            sensorBlocks -= event.block
            event.block.type = Material.AIR
        } else {
            event.isCancelled = true
        }
    }

    override val contextManager: PlayerDataAccess<Unit> = PlayerDataAccess.noop()

    override fun apply(player: Player) {
        bs.accept(player, SENSOR_BLOCK_DATA)
    }

    override fun remove(player: Player) {
        bs.remove(player)
    }

    override fun unload(external: Boolean) {
        bs.unload()
        unregisterListener(this)
    }
}

class BlockSelector(
    private val plugin: JavaPlugin,
    private val onBlockPlace: (Block) -> Unit = {}
) : IgniteResource {
    private val itemBuilder = ItemBuilder(Material.CARROT_ON_A_STICK) {
        name(Component.text("Hold to target").color(NamedTextColor.GOLD))
        unbreakable()
    }
    private var activeListener: UnloadableResource? = null
    private var rightClickListener: UnloadableResource? = null
    private val playerData: OnlinePlayerData<PlayerBlockSelectorState> = OnlinePlayerData(plugin, onEvict = {p, data ->
        data.unload()
    })

    private var item : ItemStack? = null

    override fun load() {
        activeListener = itemBuilderActiveListener(plugin, itemBuilder, ::onItemActive)
        rightClickListener = itemBuilderRightClickListener(plugin, itemBuilder, ::onItemRightClick)
        item = itemBuilder.build()
    }

    fun accept(player: Player, blockData: BlockData) {
        playerData += player to PlayerBlockSelectorState(player, blockData)
        player.give(item!!)
    }

    fun remove(player: Player) {
        playerData.remove(player)?.unload()
        player.inventory.remove(item!!)
    }

    private fun onItemActive(player: Player, active: Boolean) {
        if (player !in playerData.keys) return
        val state = playerData[player] ?: return
        if (state.used) return
        state.task?.unload()
        state.blockDisplay?.destroy(player)
        state.blockDisplay = null
        state.block = null
        if (active) {
//            state.task = GlobalResourceTrackers.scheduler.repeatTask(2L) { check(player) }
            val unloadable = object : UnloadableResource, Listener {
                @EventHandler
                private fun onMove(event: PlayerMoveEvent) {
                    check(event.player)
                }

                override fun unload(external: Boolean) {
                    unregisterListener(this)
                }
            }
            registerListener(plugin, unloadable)
            state.task = unloadable
        }
    }

    private fun check(player: Player) {
        val state = playerData[player] ?: return
        val rayTraceResult = player.rayTraceBlocks(5.0)
        if (
            rayTraceResult != null &&
            rayTraceResult.hitBlock != null &&
            rayTraceResult.hitBlock!!.isSolid
        ) {
            val targetSpot = rayTraceResult.hitPosition.add(
                player.location.direction.normalize().multiply(-0.5)
            ).toLocation(player.world)
            if (targetSpot.block.type == Material.AIR) {
                val solidGround = targetSpot.clone().subtract(0.0, 1.0, 0.0).block.isSolid
                if (solidGround) {
                    val targetBlock = targetSpot.block
                    if (state.blockDisplay == null || targetBlock != state.block) {
                        state.blockDisplay?.destroy(player)
                        val clientEntity = ClientEntity.blockDisplay(targetBlock.location.toVector())
                        clientEntity.spawn(player)
                        clientEntity.metadataUpdate(player, listOf(
                            ClientEntityMetadataHelpers.blockDisplayBlock(state.targetBlockType)
                        ))
                        state.blockDisplay = clientEntity
                        state.block = targetBlock
                    }
                }
            }
        }
    }

    private fun onItemRightClick(player: Player, i: ItemStack) {
        if (player !in playerData.keys) return
        val state = playerData[player] ?: return
        if (state.used) return
        if (state.block == null) return
        state.block!!.blockData = state.targetBlockType
        onBlockPlace(state.block!!)
        state.unload()
        state.used = true
    }

    override fun unload(external: Boolean) {
        activeListener?.unload()
        rightClickListener?.unload()
        playerData.values.forEach { it.unload() }
        playerData.unload()
        activeListener = null
        rightClickListener = null
    }
}

data class BlockSelectorOptions(
    val itemModel: NamespacedKey,
    val itemBuilderFn: ItemBuilder.() -> Unit,
    val offset: Vector? = null,
    val itemCount: Int = 1,
) {}

class FakeBlockSelector(
    private val plugin: JavaPlugin,
    private val options: BlockSelectorOptions,
    private val listener: BlockSelectorListener = object : BlockSelectorListener {}
) : IgniteResource {
    private val itemBuilder : ItemBuilder = ItemBuilder(Material.CARROT_ON_A_STICK) {
        unbreakable()
        itemModel(options.itemModel)
    }
    private var activeListener: ItemActiveListenerOperations? = null
    private var rightClickListener: UnloadableResource? = null
    private val playerData: OnlinePlayerData<FakePlayerBlockSelectorState> =
        OnlinePlayerData(plugin, onEvict = { p, data ->
            data.unload()
        })

    private var item : ItemStack? = null

    override fun load() {
        activeListener = itemBuilderActiveListenerPokeable(plugin, itemBuilder, ::onItemActive)
        rightClickListener = itemBuilderRightClickListener(plugin, itemBuilder, ::onItemRightClick)
        item = itemBuilder.build()
    }

    fun give(player: Player, amount: Int = 1) {
        val i = item!!.clone()
        i.amount = amount
        player.give(i)
    }

    fun pokeItemCheck(player: Player) {
        activeListener?.poke(player)
    }

    private fun onItemActive(player: Player, active: Boolean) {
        if (item == null) return
        if (player !in playerData.keys) {
            playerData += player to FakePlayerBlockSelectorState(player, ItemBuilder(Material.STONE) {
                itemModel(options.itemModel)
            }.build())
        }
        val state = playerData[player] ?: return
        state.task?.unload()
        state.itemDisplay?.destroy(player)
        state.itemDisplay = null
        state.block = null
        state.task = null
        if (!listener.onTryUseItemActive(player)) return
        if (active) {
//            state.task = GlobalResourceTrackers.scheduler.repeatTask(2L) { check(player) }
            val unloadable = object : UnloadableResource, Listener {
                @EventHandler
                private fun onMove(event: PlayerMoveEvent) {
                    check(event.player)
                }

                override fun unload(external: Boolean) {
                    unregisterListener(this)
                }
            }
            registerListener(plugin, unloadable)
            state.task = unloadable
        }
    }

    private fun check(player: Player) {
        val state = playerData[player] ?: return
        if (!listener.onTryUseItemActive(player)) return
        val rayTraceResult = player.rayTraceBlocks(5.0)
        if (
            rayTraceResult != null &&
            rayTraceResult.hitBlock != null &&
            rayTraceResult.hitBlock!!.isSolid
        ) {
            val targetSpot = rayTraceResult.hitPosition.add(
                player.location.direction.normalize().multiply(-0.5)
            ).toLocation(player.world)
            if (targetSpot.block.type == Material.AIR) {
                val solidGround = targetSpot.clone().subtract(0.0, 1.0, 0.0).block.isSolid
                if (solidGround) {
                    val targetBlock = targetSpot.block
                    if (state.itemDisplay == null || targetBlock != state.block) {
                        state.itemDisplay?.destroy(player)
                        val clientEntity = ClientEntity.itemDisplay(
                            targetBlock.location.toVector().add(options.offset ?: Vector())
                        )
                        clientEntity.spawn(player)
                        clientEntity.metadataUpdate(player, listOf(
                            ClientEntityMetadataHelpers.itemDisplayItem(state.istack)
                        ))
                        state.itemDisplay = clientEntity
                        state.block = targetBlock
                    }
                }
            }
        }
    }

    private fun onItemRightClick(player: Player, i: ItemStack) {
        if (player !in playerData.keys) return
        val state = playerData[player] ?: return
        if (!listener.onTryUseRightClick(player)) return
        if (state.block == null) return
        val idisplay = state.block!!.world.spawn(
            state.block!!.location.add(options.offset ?: Vector()),
            ItemDisplay::class.java
        )
        idisplay.setItemStack(state.istack)
        state.block!!.type = Material.BARRIER
        listener.onBlockPlace(PlaceContext(idisplay, player, i))
        state.unload()
    }

    override fun unload(external: Boolean) {
        activeListener?.unload()
        rightClickListener?.unload()
        playerData.values.forEach { it.unload() }
        playerData.unload()
        activeListener = null
        rightClickListener = null
    }

    data class PlaceContext(val itemDisplay: ItemDisplay, val player: Player, val item: ItemStack)
    interface BlockSelectorListener {
        fun onBlockPlace(placeContext: PlaceContext) {}
        fun onTryUseItemActive(player: Player) : Boolean = true
        fun onTryUseRightClick(player: Player) : Boolean = true
    }
}

private class PlayerBlockSelectorState(
    val player : Player,
    val targetBlockType : BlockData,
    var blockDisplay: ClientEntity? = null,
    var task: UnloadableResource? = null,
    var block: Block? = null,
    var used: Boolean = false
) : UnloadableResource {
    override fun unload(external: Boolean) {
        blockDisplay?.destroy(player)
        task?.unload()
    }
}

private class FakePlayerBlockSelectorState(
    val player : Player,
    val istack : ItemStack,
    var itemDisplay: ClientEntity? = null,
    var task: UnloadableResource? = null,
    var block: Block? = null
) : UnloadableResource {
    override fun unload(external: Boolean) {
        itemDisplay?.destroy(player)
        task?.unload()
    }
}
