package com.empire.forest.mechanic

import com.empire.forest.ForestContext
import com.empire.forest.perk.BuckshotPerk
import com.empire.forest.resourcepack.ResourcePackConstants
import com.empire.ignite.util.GlobalResourceTrackers
import com.empire.ignite.util.IgniteResource
import com.empire.ignite.util.callback.ForwardCallbackRegistration
import com.empire.ignite.util.entity.EntityDecorator
import com.empire.ignite.util.entity.EntityModelBuilder
import com.empire.ignite.util.item.ItemBuilder
import com.empire.ignite.util.registerListener
import com.empire.ignite.util.unregisterListener
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Entity
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector

class BearTrap(
    private val plugin: JavaPlugin,
    private val context: ForestContext,
    private val planter: Entity?,
    private val location: Location,
    private val trapType: Type = Type.SURVIVOR
) : IgniteResource, Listener {
    enum class Type {
        SURVIVOR, HUNTER
    }
    companion object {
        private fun bearTrapEMB(builder: ItemBuilder) : EntityModelBuilder<ItemDisplay> =
            EntityModelBuilder(ItemDisplay::class.java) {
                onSpawn(
                    EntityDecorator<ItemDisplay> {
                        direct {  id ->
                            id.setItemStack(builder.build())
                        }
                    }.build()
                )
            }
    }
    private val survivorEMB = bearTrapEMB(ResourcePackConstants.BEAR_TRAP_OPEN_ITEM_SURVIVOR)
    private val hunterEMB = bearTrapEMB(ResourcePackConstants.BEAR_TRAP_OPEN_ITEM_HUNTER)
    private var itemDisplay : Entity? = null
    private val callbacks: MutableList<BearTrapCallback> = mutableListOf()

    fun onTrap(callback: BearTrapCallback) {
        callbacks += callback
    }

    override fun load() {
        registerListener(plugin, this)
        itemDisplay = (if (trapType == Type.SURVIVOR) survivorEMB else hunterEMB)
            .spawn(location.clone().add(Vector(0.0F, 0.5F, 0.0F)))
    }

    @EventHandler
    private fun onMove(event: PlayerMoveEvent) {
        if (event.player !in context.playerAccess.alive) return
        if (event.to.distanceSquared(location) > 0.3) return
        val player = event.player
        player.playSound(player.location.clone(), Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0F, 0.4F)
        val dmg = 3.5
        if (planter == null) player.damage(dmg)
        else player.damage(dmg, planter)

        freeze(player)

        Bukkit.getScheduler().runTaskLater(GlobalResourceTrackers.plugin!!, Runnable {
            unfreeze(player)
        }, 60L)
        callbacks.forEach { callback -> callback.onTrap(this) }
        unload()
    }

    private fun freeze(player: Player) {
        player.getAttribute(Attribute.MOVEMENT_SPEED)?.let { it.baseValue = 0.0 }
        player.getAttribute(Attribute.JUMP_STRENGTH)?.let { it.baseValue = 0.0 }
    }

    private fun unfreeze(player: Player) {
        player.getAttribute(Attribute.MOVEMENT_SPEED)?.let { it.baseValue = 0.10000000149011612 }
        player.getAttribute(Attribute.JUMP_STRENGTH)?.let { it.baseValue = it.defaultValue }
    }

    override fun unload(external: Boolean) {
        unregisterListener(this)
        itemDisplay?.remove()
        itemDisplay = null
    }
}

fun interface BearTrapCallback {
    fun onTrap(trap: BearTrap)
}