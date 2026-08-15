package com.empire.forest.mechanic

import com.empire.ignite.game.kit.ability.AbilityCooldown
import com.empire.ignite.util.GlobalResourceTrackers
import com.empire.ignite.util.UnloadableResource
import com.empire.ignite.util.collection.OnlinePlayerSet
import com.empire.ignite.util.item.ItemBuilder
import com.empire.ignite.util.itemBuilderRightClickListener
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class CooldownRightClickAbility(
    plugin: JavaPlugin, cooldownTicks: Long,
    private val options: CooldownRightClickAbilityOptions,
    itemBuilder: ItemBuilder,
    private val effect: (Player) -> Unit
) : UnloadableResource {
    private val onlinePlayerSet: OnlinePlayerSet = OnlinePlayerSet()
    private val cooldown: AbilityCooldown = AbilityCooldown(plugin, cooldownTicks)
    private val dump = GlobalResourceTrackers.createResourceDump()

    init {
        onlinePlayerSet.load()
        dump.add(onlinePlayerSet)
        dump.add(itemBuilderRightClickListener(plugin, itemBuilder, ::onRightClick))
        dump.add(object : UnloadableResource {
            override fun unload(external: Boolean) {
                cooldown.unload()
            }
        })
    }

    fun add(player: Player) {
        onlinePlayerSet.add(player)
    }

    fun remove(player: Player) {
        onlinePlayerSet.remove(player)
    }

    private fun onRightClick(player: Player, item: ItemStack) {
        if (player !in onlinePlayerSet) return
        if (!cooldown.use(player)) {
            val message = cooldown.genericCooldownMessage(player)
            if (message != null) player.sendMessage(message)
            return
        }
        effect(player)
        if (options.consume) {
            item.amount -= 1
        }
    }

    override fun unload(external: Boolean) {
        dump.destroyAll()
    }
}

data class CooldownRightClickAbilityOptions(val consume: Boolean = true)