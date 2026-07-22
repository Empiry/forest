package com.empire.forest.perk

import com.empire.forest.ForestContext
import com.empire.ignite.util.GlobalResourceTrackers
import com.empire.ignite.util.IgniteResource
import com.empire.ignite.util.UnloadableResource
import com.empire.ignite.util.collection.OnlinePlayerData
import com.empire.ignite.util.playSoundToPlayer
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

interface ForestPerk : UnloadableResource {
    fun apply(player: Player)
    fun remove(player: Player)
}

class HeartbeatPerk(
    plugin: JavaPlugin,
    private val context: ForestContext
) : ForestPerk {
    private val applied : OnlinePlayerData<HeartbeatTask?> = OnlinePlayerData(plugin, onEvict = { _, task ->
        task?.unload()
    })
    private val task = GlobalResourceTrackers.scheduler.repeatTask(1L, ::heartbeat)

    override fun apply(player: Player) {
        applied += (player to null)
    }

    fun heartbeat() {
        for ((player, task) in applied.entries.toSet()) {
            if (player !in context.survivorTeam.players) continue
            val distance = context.hunterTeam.players.minOfOrNull {
                hunter -> hunter.location.distanceSquared(player.location)
            }
            if (distance == null || distance > 30*30) {
                task?.unload()
                applied[player] = null
                continue
            }
            var beatFrequency = 12
            if (distance < 10*10) {
                beatFrequency = 6
            } else if (distance < 20*20) {
                beatFrequency = 8
            }
            if (beatFrequency != task?.interval) {
                task?.unload()
                val newTask = HeartbeatTask(player, beatFrequency)
                newTask.load()
                applied[player] = newTask
            }
        }
    }

    override fun remove(player: Player) {
        applied.remove(player)?.unload()
    }

    override fun unload(external: Boolean) {
        applied.values.forEach { heartbeatTask -> heartbeatTask?.unload(external) }
        applied.unload(external)
        task.unload(external)
    }
}

private class HeartbeatTask(
    private val player: Player,
    val interval: Int
) : IgniteResource {
    private var task : UnloadableResource? = null

    override fun load() {
        task = GlobalResourceTrackers.scheduler.repeatTask(interval.toLong()) {
            playSoundToPlayer(player, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.0f, 0.9f)
            GlobalResourceTrackers.scheduler.after(2L) {
                playSoundToPlayer(player, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.0f, 0.1f)
            }
        }
    }

    override fun unload(external: Boolean) {
        task?.unload()
        task = null
    }
}