package com.empire.forest.perk

import com.empire.forest.ForestContext
import com.empire.forest.util.PlayerDataAccess
import com.empire.ignite.util.collection.OnlinePlayerData
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class RevivePerk(
    plugin: JavaPlugin,
    private val context: ForestContext
) : ForestPerk<Unit> {
    override val contextManager: PlayerDataAccess<Unit> = PlayerDataAccess.noop()
    private val data : OnlinePlayerData<MedicPlayerData> = OnlinePlayerData(plugin)

    override fun apply(player: Player) {
        data += player to MedicPlayerData(player)
    }

    fun using(player: Player) : Boolean =
        player in data.keys

    override fun remove(player: Player) {
        data -= player
    }

    override fun unload(external: Boolean) {
        data.unload(external)
    }
}

private data class MedicPlayerData(val player: Player) {}