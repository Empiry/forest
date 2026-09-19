package com.empire.forest.perk

import com.empire.hacks.common.builder.ClientEntityMetadataHelpers
import com.empire.ignite.util.*
import com.empire.ignite.util.collection.OnlinePlayerData
import com.empire.ignite.util.entity.ClientEntity
import com.empire.ignite.util.item.ItemBuilder
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector

data class BlockSelectorOptions(
    val selectorItemModel: NamespacedKey,
    val shadowItemModel: NamespacedKey = selectorItemModel,
    val itemBuilderFn: ItemBuilder.() -> Unit,
    val offset: Vector? = null,
    val itemCount: Int = 1,
    val snap: Boolean = true
)

class FakeBlockSelector(
    private val plugin: JavaPlugin,
    private val options: BlockSelectorOptions,
    private val listener: BlockSelectorListener = object : BlockSelectorListener {}
) : IgniteResource {
    private val itemBuilder : ItemBuilder = ItemBuilder(Material.CARROT_ON_A_STICK) {
        unbreakable()
        itemModel(options.selectorItemModel)
    }
    private var activeListener: ItemActiveListenerOperations? = null
    private var rightClickListener: UnloadableResource? = null
    private val playerData: OnlinePlayerData<FakePlayerBlockSelectorState> =
        OnlinePlayerData(plugin, onEvict = { p, data ->
            data.unload()
        })

    private var item : ItemStack? = null

    init {
        options.itemBuilderFn(itemBuilder)
    }

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
            playerData += player to FakePlayerBlockSelectorState(player,
        ItemBuilder(Material.STONE) {
                        itemModel(options.shadowItemModel)
                    }.build(),
                ItemBuilder(Material.STONE) {
                    itemModel(options.selectorItemModel)
                }.build()
            )
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
        val rayTraceResult = player.rayTraceBlocks(5.0) ?: return
        if (
            rayTraceResult.hitBlock != null &&
            rayTraceResult.hitBlock!!.isSolid
        ) {
            val targetSpot = rayTraceResult.hitPosition.add(
                player.location.direction.normalize().multiply(-0.5)
            ).toLocation(player.world)
            if (targetSpot.block.type == Material.AIR) {
                val targetSpotBlock = targetSpot.clone().subtract(0.0, 1.0, 0.0).block
                val solidGround = targetSpotBlock.isSolid
                if (solidGround && targetSpotBlock.type != Material.BARRIER) {
                    val targetBlock = targetSpot.block
                    if (state.itemDisplay == null || targetBlock != state.block) {
                        state.itemDisplay?.destroy(player)
                        val clientEntity = ClientEntity.itemDisplay(
                            targetBlock.location.toVector().add(options.offset ?: Vector())
                        )
                        clientEntity.spawn(player)
                        clientEntity.metadataUpdate(player, listOf(
                            ClientEntityMetadataHelpers.itemDisplayItem(state.shadowIStack)
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
        idisplay.setItemStack(state.emplaceIStack)
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
    val shadowIStack : ItemStack,
    val emplaceIStack : ItemStack,
    var itemDisplay: ClientEntity? = null,
    var task: UnloadableResource? = null,
    var block: Block? = null
) : UnloadableResource {
    override fun unload(external: Boolean) {
        itemDisplay?.destroy(player)
        task?.unload()
    }
}
