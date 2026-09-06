package com.empire.forest.mechanic.stamina

import com.empire.forest.ForestContext
import com.empire.ignite.display.Displays
import com.empire.ignite.util.ChangingSetReceiver
import com.empire.ignite.util.GlobalResourceTrackers
import com.empire.ignite.util.IgniteResource
import com.empire.ignite.util.collection.OnlinePlayerData
import com.empire.ignite.util.registerListener
import com.empire.ignite.util.unregisterListener
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.java.JavaPlugin
import kotlin.math.max
import kotlin.math.min

class SurvivorStamina(
    private val plugin: JavaPlugin,
    private val context: ForestContext,
    drainSeconds: Int, regainSeconds: Int
) : IgniteResource, Listener {
    private val data : OnlinePlayerData<PlayerStaminaData> = OnlinePlayerData(plugin)
    private val drainPerTick = 100.0 / (drainSeconds*20)
    private val regainPerTick = 100.0 / (regainSeconds*20)
    private val xpBarData : OnlinePlayerData<Long> = OnlinePlayerData(plugin, onEvict = { p, id ->
        Displays.getXPBar(p).cancelTask(id)
    })
    private val dump = GlobalResourceTrackers.createResourceDump()

    override fun load() {
        registerListener(plugin, this)
        context.survivorTeamChangeSetContract.changeSet.setupOnAdd(object : ChangingSetReceiver<Player> {
            override fun onAdd(item: Player) {
                setupActionBar(item)
            }
        })
        context.survivorTeamChangeSetContract.changeSet.setupOnRemove(object : ChangingSetReceiver<Player> {
            override fun onRemove(item: Player) {
                disableActionBar(item)
            }
        })
        context.survivorTeam.players.forEach {
            setupActionBar(it)
        }
        dump.add(
            GlobalResourceTrackers.scheduler.repeat(1L) {
                context.survivorTeam.players.forEach { survivor ->
                    val survivorData = data[survivor] ?: return@forEach
                    if (!survivorData.moving) {
                        survivorData.staminaLevel = min(100.0, survivorData.staminaLevel + regainPerTick)
                    }
                    survivorData.moving = false
                }
            }
        )
    }

    private fun setupActionBar(player: Player) {
        if (xpBarData[player] != null && xpBarData[player] != -1L) return
        xpBarData[player] = Displays.getXPBar(player).scheduleImmediateTimed {
            0 to ((data[player]?.staminaLevel ?: 0.0) / 100.0).toFloat()
        }
    }

    private fun disableActionBar(player: Player) {
        val xpTask = xpBarData[player] ?: return
        if (xpTask != -1L) {
            Displays.getXPBar(player).cancelTask(xpTask)
            xpBarData[player] = -1L
        }
    }

    @EventHandler
    private fun onMove(event: PlayerMoveEvent) {
        if (event.player !in context.survivorTeam.players) return
        if (event.player !in data.keys) {
            data[event.player] = PlayerStaminaData()
        }
        val playerData = data[event.player] ?: return
        playerData.moving = true
        if (playerData.staminaLevel > 20 && event.player.foodLevel < 20) {
            event.player.foodLevel = 20
        } else if (playerData.staminaLevel <= 0 && event.player.foodLevel > 0) {
            event.player.foodLevel = 3
        }
        if (event.player.isSprinting) {
            playerData.staminaLevel = max(0.0, playerData.staminaLevel - drainPerTick)
        } else if (event.player.isSneaking) {
            playerData.staminaLevel = min(100.0, playerData.staminaLevel + regainPerTick)
        }
    }

    override fun unload(external: Boolean) {
        unregisterListener(this)
        data.unload(external)
        xpBarData.unload(external)
        dump.destroyAll()
    }
}

private data class PlayerStaminaData(
    var staminaLevel : Double = 100.0,
    var moving : Boolean = false
)