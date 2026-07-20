package com.empire.forest.mechanic

import com.empire.forest.ForestContext
import com.empire.forest.resourcepack.ResourcePackConstants
import com.empire.ignite.util.IgniteResource
import com.empire.ignite.util.entity.EntityDecorator
import com.empire.ignite.util.entity.EntityModelBuilder
import com.empire.ignite.util.registerListener
import com.empire.ignite.util.unregisterListener
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.EulerAngle
import org.bukkit.util.Vector

class BearTrap(
    private val plugin: JavaPlugin,
    private val context: ForestContext,
    private val planter: Entity?,
    private val location: Location
) : IgniteResource, Listener {
    private val emb = EntityModelBuilder(ArmorStand::class.java) {
        onSpawn(
            EntityDecorator<ArmorStand> {
                direct {  armorStand ->
                    armorStand.setGravity(false)
                    armorStand.setAI(false)
                    armorStand.isInvulnerable = true
                    armorStand.isInvisible = true
                    armorStand.setBasePlate(false)
                    armorStand.setItem(EquipmentSlot.HAND, ResourcePackConstants.BEAR_TRAP_OPEN_ITEM.build())
                    armorStand.rightArmPose = EulerAngle(0.0, 180.0, 0.0)
                }
            }.build()
        )
    }
    private var armorstand : ArmorStand? = null

    override fun load() {
        registerListener(plugin, this)
        armorstand = emb.spawn(location.clone().subtract(Vector(0.0F, 0.8F, 0.0F)))
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
        player.addPotionEffect(
            PotionEffect(PotionEffectType.SLOWNESS, 60, 120, true, false)
        )
        unload()
    }

    override fun unload(external: Boolean) {
        unregisterListener(this)
        armorstand?.remove()
        armorstand = null
    }
}