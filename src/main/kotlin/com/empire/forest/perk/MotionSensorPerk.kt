package com.empire.forest.perk

import com.empire.forest.ForestContext
import com.empire.hacks.common.builder.ClientEntityMetadataHelpers
import com.empire.hacks.common.builder.UpdateEntityMetadataConverter
import com.empire.hacks.common.builder.UpdateEntityMetadataPacketBuilder
import com.empire.ignite.util.*
import com.empire.ignite.util.collection.OnlinePlayerData
import com.empire.ignite.util.entity.ClientEntity
import com.empire.ignite.util.item.ItemBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameEvent
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
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
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

class MotionSensorPerk(
    plugin: JavaPlugin,
    private val context: ForestContext
) : ForestPerk, Listener {
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
            fakeGlow(context.hunterTeam.players.toList() + survivor, survivor, 15L * 20L)
            sensorBlocks -= event.block
            event.block.type = Material.AIR
        } else {
            event.isCancelled = true
        }
    }

    override fun apply(player: Player) {
        bs.accept(player, SENSOR_BLOCK_DATA)
    }

    override fun remove(player: Player) {
        bs.remove(player)
    }

    private fun fakeGlow(recipients: List<Player>, player: Player, duration: Long) {
        val glowByte : Byte = 0x40
        val glowOnPacket = UpdateEntityMetadataPacketBuilder(player, listOf(
            UpdateEntityMetadataConverter.MainEntityByteMask { mask ->
                mask or glowByte
            }
        ))
        val glowOffPacket = UpdateEntityMetadataPacketBuilder(player, listOf(
            UpdateEntityMetadataConverter.MainEntityByteMask { mask ->
                mask and (glowByte.inv())
            }
        ))
        val unloadable = HACKS.overrideEntityMetadata(recipients, player, glowOnPacket)

        // force-update before data-watcher kicks in
        recipients.forEach { r -> HACKS.sendDeltaUpdateEntityMetadataPacket(r, glowOnPacket) }

        GlobalResourceTrackers.scheduler.after(duration) {
            unloadable.release()
            // and same here (todo: this should be 'no-op'/regenerate server-side state)
            recipients.forEach { r -> HACKS.sendDeltaUpdateEntityMetadataPacket(r, glowOffPacket) }
        }
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
    companion object {
        private val ITEM = ItemBuilder(Material.CARROT_ON_A_STICK) {
            name(Component.text("Hold to target").color(NamedTextColor.GOLD))
            unbreakable()
        }
    }
    private var activeListener: UnloadableResource? = null
    private var rightClickListener: UnloadableResource? = null
    private val playerData: OnlinePlayerData<PlayerBlockSelectorState> = OnlinePlayerData(plugin, onEvict = {p, data ->
        data.unload()
    })

    private var item : ItemStack? = null

    override fun load() {
        activeListener = itemBuilderActiveListener(plugin, ITEM, ::onItemActive)
        rightClickListener = itemBuilderRightClickListener(plugin, ITEM, ::onItemRightClick)
        item = ITEM.build()
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