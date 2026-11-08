package com.empire.forest.mechanic.survivalist

import com.empire.forest.ForestContext
import com.empire.ignite.game.kit.IgniteAbilityV2
import com.empire.ignite.util.collection.OnlinePlayerData
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
    private val data: OnlinePlayerData<TallGrassPlayerData> = OnlinePlayerData(context.plugin)

    override fun enable() {
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(context.plugin, {
            checkTallGrass(player)
        }, 0L, 2L)
    }

    companion object {
        private val BLOCK_TYPES = listOf(
            Material.TALL_GRASS, Material.LARGE_FERN,
            Material.ROSE_BUSH, Material.PEONY,
            Material.PITCHER_PLANT, Material.LILAC
        )
    }

    private fun checkTallGrass(player: Player) {
        if (player.location.block.type !in BLOCK_TYPES) return
        if (data[player] == null) {
            data[player] = TallGrassPlayerData()
        }
        val playerData = data[player] ?: return
        if (!player.isSneaking) {
            playerData.stayTicks = 0
            player.removePotionEffect(PotionEffectType.INVISIBILITY)
            return
        }
        playerData.stayTicks += 2
        if (playerData.stayTicks < 40) return
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
//        playerData.stayTicks = 0
    }

    override fun disable() {
        if (taskID != -1) {
            Bukkit.getScheduler().cancelTask(taskID)
            taskID = -1
        }
        data.unload()
    }
}

private class TallGrassPlayerData(var stayTicks: Int = 0)