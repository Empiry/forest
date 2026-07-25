package com.empire.forest.mechanic.phantom

import com.empire.forest.perk.ForestPerk
import com.empire.ignite.game.kit.ability.AbilityCooldown
import com.empire.ignite.util.GlobalResourceTrackers
import com.empire.ignite.util.UnloadableResource
import com.empire.ignite.util.collection.OnlinePlayerSet
import com.empire.ignite.util.item.ItemBuilder
import com.empire.ignite.util.itemBuilderRightClickListener
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class CloakingPerk(
    plugin: JavaPlugin
) : ForestPerk {
    companion object {
        private val ITEM_BUILDER = ItemBuilder(Material.PURPLE_STAINED_GLASS_PANE) {
            name(Component.text("Cloaking item").color(NamedTextColor.DARK_BLUE))
            lore(
                listOf(
                    Component.text("Hides you for 5 seconds").color(NamedTextColor.BLUE),
                    Component.text("25 second cooldown").color(NamedTextColor.RED),
                )
            )
        }
    }
    private val onlinePlayerSet: OnlinePlayerSet = OnlinePlayerSet()
    private val item : ItemStack
    private val cooldown: AbilityCooldown = AbilityCooldown(plugin, 20L * 25L)
    private val dump = GlobalResourceTrackers.createResourceDump()

    init {
        onlinePlayerSet.load()
        dump.add(onlinePlayerSet)
        dump.add(itemBuilderRightClickListener(plugin, ITEM_BUILDER, ::onRightClick))
        item = ITEM_BUILDER.build()
        item.amount = 4
        dump.add(object : UnloadableResource {
            override fun unload(external: Boolean) {
                cooldown.unload()
            }
        })
    }

    override fun apply(player: Player) {
        player.give(item)
        onlinePlayerSet.add(player)
    }

    private fun onRightClick(player: Player, item: ItemStack) {
        if (player !in onlinePlayerSet) return
        if (!cooldown.use(player)) {
            val message = cooldown.genericCooldownMessage(player)
            if (message != null) player.sendMessage(message)
            return
        }
        player.addPotionEffect(
            PotionEffect(
                PotionEffectType.INVISIBILITY, 20 * 5, 1, true, false
            )
        )

        item.amount -= 1
    }

    override fun remove(player: Player) {
        onlinePlayerSet.remove(player)
    }

    override fun unload(external: Boolean) {
        dump.destroyAll()
    }
}