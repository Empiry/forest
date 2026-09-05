package com.empire.forest.perk

import com.empire.forest.ForestContext
import com.empire.forest.util.BoundedLRUAccessor
import com.empire.forest.util.PlayerDataAccess
import com.empire.ignite.game.kit.ability.AbilityCooldown
import com.empire.ignite.game.kit.ability.AbilityUseListener
import com.empire.ignite.util.GlobalResourceTrackers
import com.empire.ignite.util.UnloadableResource
import com.empire.ignite.util.UnloadableResourcePostHook
import com.empire.ignite.util.collection.OnlinePlayerData
import com.empire.ignite.util.collection.OnlinePlayerSet
import com.empire.ignite.util.item.ItemBuilder
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector

class NewMotionSensorPerk(
    plugin: JavaPlugin,
    private val context: ForestContext,
) : ForestPerk<Unit>, Listener {
    override val contextManager: PlayerDataAccess<Unit> = PlayerDataAccess.noop()
    private val dump = GlobalResourceTrackers.createResourceDump()
    val playerData: OnlinePlayerData<MotionSensorPlayerData> = OnlinePlayerData(
        plugin, onEvict = { _, d -> d.unload() }
    )
    val cooldown: AbilityCooldown = AbilityCooldown(
        plugin, 5L * 20L, abilityUseListeners = listOf(
            object : AbilityUseListener {
                override fun onAbilityCanUse(player: Player) {
                    selector.pokeItemCheck(player)
                }
            }
        )
    )
    private val selector = FakeBlockSelector(
        plugin,
        options = BlockSelectorOptions(
            itemModel = NamespacedKey("horror", "objects/motion_sensor_on"),
            itemBuilderFn = {
                name(Component.text("Motion Sensor").color(NamedTextColor.RED))
            },
            offset = Vector(0.5, 0.5, 0.5),
        ),
        object : FakeBlockSelector.BlockSelectorListener {
            override fun onBlockPlace(placeContext: FakeBlockSelector.PlaceContext) {
                this@NewMotionSensorPerk.blockPlace(placeContext)
            }

            override fun onTryUseItemActive(player: Player): Boolean {
                return this@NewMotionSensorPerk.tryUse(player, verbose = false)
            }

            override fun onTryUseRightClick(player: Player): Boolean {
                return this@NewMotionSensorPerk.tryUse(player, verbose = true)
            }
        }
    )

    init {
        selector.load()
    }

    override fun apply(player: Player) {
        selector.give(player)
        playerData[player] = MotionSensorPlayerData(BoundedLRUAccessor(1))
    }

    fun using(player: Player) : Boolean =
        player in playerData.keys

    private fun blockPlace(blockPlaceContext: FakeBlockSelector.PlaceContext) {
        val playerDataEntry = playerData[blockPlaceContext.player]
        if (playerDataEntry == null) {
            blockPlaceContext.itemDisplay.remove()
            return
        }
        val apparatus = MotionSensorApparatus(context, blockPlaceContext.itemDisplay)
        dump.addSelfTracking(apparatus)
        when (val result = playerDataEntry.buf.emplaceItem(UnloadableResource.fromPostHook(apparatus))) {
            is BoundedLRUAccessor.Result.Replaced<UnloadableResource> -> {
                result.item.unload()
            }
            is BoundedLRUAccessor.Result.Success<*> -> {}
        }
        cooldown.use(blockPlaceContext.player)
    }

    private fun tryUse(player: Player, verbose: Boolean = false) : Boolean {
        if (player !in playerData.keys) return false
        val result = cooldown.canUse(player)
        if (verbose) {
            cooldown.genericCooldownMessage(player)?.let {
                player.sendMessage(it)
            }
        }
        return result
    }

    override fun remove(player: Player) {}

    override fun unload(external: Boolean) {
        selector.unload(external)
        dump.destroyAll()
        cooldown.unload()
    }
}

private class MotionSensorApparatus(
    private val context: ForestContext,
    private val idisplay: ItemDisplay
) : UnloadableResourcePostHook {
    private val hooks: MutableList<() -> Unit> = mutableListOf()
    private val dump = GlobalResourceTrackers.createResourceDump()
    private val detected = OnlinePlayerSet()
    private var ticks = 0L

    init {
        dump.add(
            GlobalResourceTrackers.scheduler.repeatTask(5L) {
                ticks += 5L
                val inRangePlayers = context.survivorTeam.players.filter { player ->
                    player.location.distanceSquared(idisplay.location) <= 16 * 16 && !player.isSneaking
                }
                (inRangePlayers - detected).forEach { newDetected ->
                    newDetected.addPotionEffect(
                        PotionEffect(
                            PotionEffectType.GLOWING, 40, 0,
                            true, false
                        )
                    )
                    detected += newDetected
                }
                val inRange = inRangePlayers.isNotEmpty()
                if (ticks % 15L == 0L && inRange) {
                    context.world!!.playSound(
                        idisplay.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.5f, 2f
                    )
                }
                detected.removeIf { it !in inRangePlayers }
                val clone = idisplay.itemStack.clone()
                val im = clone.itemMeta
                im.itemModel =
                    if (inRange) NamespacedKey("horror", "objects/motion_sensor_on")
                    else NamespacedKey("horror", "objects/motion_sensor_off")
                clone.itemMeta = im
                idisplay.setItemStack(clone)
            }
        )
        detected.load()
    }

    override fun unloadWithPostHook() {
        dump.destroyAll()
        if (idisplay.location.block.type == Material.BARRIER) {
            idisplay.location.block.type = Material.AIR
        }
        idisplay.remove()
        hooks.forEach { it() }
        detected.unload()
    }

    override fun addPostHook(hook: () -> Unit) {
        hooks.add(hook)
    }
}

data class MotionSensorPlayerData(
    val buf: BoundedLRUAccessor<UnloadableResource>
) : UnloadableResource {
    override fun unload(external: Boolean) {
        buf.buffer.forEach { rs -> rs.unload(external) }
        buf.clear()
    }
}