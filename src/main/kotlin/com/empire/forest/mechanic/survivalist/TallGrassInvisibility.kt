package com.empire.forest.mechanic.survivalist

import com.empire.forest.ForestContext
import com.empire.ignite.game.kit.IgniteAbilityV2
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class TallGrassInvisibility(
    override val player: Player,
    private val context: ForestContext
) : IgniteAbilityV2, Listener {
    private var taskID = -1
    override fun enable() {
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(context.plugin, {
            checkTallGrass(player)
        }, 0L, 2L)
    }

    private fun checkTallGrass(player: Player) {
        if (player.location.block.type != Material.TALL_GRASS) return
        if (!player.isSneaking) return
        val existingEffect = player.activePotionEffects.firstOrNull {
            effect -> effect.type == PotionEffectType.INVISIBILITY
        }
        val duration = existingEffect?.duration
        if (duration != null && duration < 4) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY)
        }
        player.addPotionEffect(
            PotionEffect(PotionEffectType.INVISIBILITY, 10, 1, true, false)
        )
    }

    override fun disable() {
        if (taskID != -1) {
            Bukkit.getScheduler().cancelTask(taskID)
            taskID = -1
        }
    }
}