package com.empire.forest.perk

import com.empire.forest.ForestContext
import com.empire.forest.mechanic.CooldownRightClickAbility
import com.empire.forest.mechanic.CooldownRightClickAbilityOptions
import com.empire.forest.util.ForestUtils
import com.empire.forest.util.PlayerDataAccess
import com.empire.ignite.util.GlobalResourceTrackers
import com.empire.ignite.util.collection.OnlinePlayerData
import com.empire.ignite.util.denyItemDrop
import com.empire.ignite.util.item.ItemBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector

class ShadowPerk(
    private val plugin: JavaPlugin,
    private val context: ForestContext
) : ForestPerk<Unit> {
    override val contextManager: PlayerDataAccess<Unit> = PlayerDataAccess.noop()
    private val shadowIMB = ItemBuilder(Material.FEATHER) {
        name(Component.text("The Shadows").color(NamedTextColor.LIGHT_PURPLE))
        itemModel(NamespacedKey("horror", "perks/stealthy"))
        unbreakable()
    }
    private val dump = GlobalResourceTrackers.createResourceDump()
    val cooldown = CooldownRightClickAbility(
        plugin, 20L * 45L,
        options = CooldownRightClickAbilityOptions(consume = false),
        itemBuilder = shadowIMB,
        ::onUse
    )
    private val istack: ItemStack
    private val data : OnlinePlayerData<Boolean> = OnlinePlayerData(plugin)

    init {
        dump.add(denyItemDrop(plugin, shadowIMB))
        istack = shadowIMB.build()
    }

    override fun apply(player: Player) {
        player.give(istack)
        cooldown.add(player)
    }

    fun cloaked(player: Player) : Boolean {
        if (player !in data.keys) return false
        return data[player]!!
    }

    private fun onUse(player: Player) {
        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.INVISIBILITY, 20 * 5, 0,
                true, false
            )
        )
        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.WEAKNESS, 20 * 5, 120,
                true, false
            )
        )
        player.getAttribute(Attribute.MOVEMENT_SPEED)?.let {
            it.baseValue = 0.10000000149011612 * 1.25
        }
        smoke(player)
        data[player] = true
        dump.add(
            GlobalResourceTrackers.scheduler.afterUntracked(5L * 20L) {
                data[player] = false
                player.getAttribute(Attribute.MOVEMENT_SPEED)?.let {
                    it.baseValue = context.getNormalMovementSpeed(player)
                }
                smoke(player)
                context.world!!.playSound(
                    player.location, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1.0F, 1.0F
                )
                ForestUtils.freezePlayer(player)
                GlobalResourceTrackers.scheduler.afterUntracked(1L * 20L) {
                    ForestUtils.resetPlayer(context, player)
                }
            }
        )
    }

    private fun smoke(player: Player) {
        context.world!!.spawnParticle(
            Particle.CAMPFIRE_COSY_SMOKE,
            player.location.clone().add(Vector(0, 1, 0)), 35,
            0.2, 0.4, 0.2,
            0.03
        )
    }

    override fun remove(player: Player) {
        cooldown.remove(player)
    }

    override fun unload(external: Boolean) {
        cooldown.unload(external)
        dump.destroyAll()
        data.unload(external)
    }
}