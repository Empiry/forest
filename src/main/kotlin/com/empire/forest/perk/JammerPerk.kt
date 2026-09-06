package com.empire.forest.perk

import com.empire.forest.ForestContext
import com.empire.forest.mechanic.CooldownRightClickAbility
import com.empire.forest.mechanic.CooldownRightClickAbilityOptions
import com.empire.forest.util.PlayerDataAccess
import com.empire.ignite.util.GlobalResourceTrackers
import com.empire.ignite.util.item.ItemBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class JammerPerk(
    private val plugin: JavaPlugin,
    private val context: ForestContext
) : ForestPerk<Unit> {
    override val contextManager: PlayerDataAccess<Unit> = PlayerDataAccess.noop()

    private val itemBuilder = ItemBuilder(Material.REPEATER) {
        name(Component.text("Jammer").color(NamedTextColor.RED))
        lore(listOf(Component.text("Breaks navigation & survivor scoreboard")))
        unbreakable()
    }
    private val cooldownAbility : CooldownRightClickAbility
    private val item : ItemStack

    init {
        cooldownAbility = CooldownRightClickAbility(
            plugin, 20L * 45L,
            CooldownRightClickAbilityOptions(consume = false), itemBuilder,
            this::use
        )
        item = itemBuilder.build()
    }

    override fun apply(player: Player) {
        cooldownAbility.add(player)
        player.give(item)
    }

    private fun use(player: Player) {
        context.jammed = true
        GlobalResourceTrackers.scheduler.after(20L * 15L) {
            context.jammed = false
            context.survivorTeam.players.forEach {
                context.scoreboard?.refresh(it)
                context.gameBossBar?.render()
            }
        }
        context.survivorTeam.players.forEach {
            context.scoreboard?.refresh(it)
            context.gameBossBar?.render()
        }
        player.sendMessage(Component.text("Jammed survivor navigation!", NamedTextColor.GREEN))
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
    }

    override fun remove(player: Player) {
        cooldownAbility.remove(player)
    }

    override fun unload(external: Boolean) {
        cooldownAbility.unload(external)
    }
}

object JammerUtils {
    fun jam(shape: ComponentShape) : Component {
        return Component.text("a".repeat(shape.charCount)).color(shape.color)
            .decorate(TextDecoration.OBFUSCATED)
    }
}

data class ComponentShape(
    val charCount: Int,
    val color: TextColor
)