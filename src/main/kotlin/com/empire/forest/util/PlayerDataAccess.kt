package com.empire.forest.util

import com.empire.ignite.util.UnloadableResource
import com.empire.ignite.util.collection.OnlinePlayerData
import org.bukkit.entity.Player

interface PlayerDataAccess<T> : UnloadableResource {
    fun getData(player: Player): T?
    fun setData(player: Player, data: T)
    fun players() : Set<Player>

    companion object {
        fun noop() : PlayerDataAccess<Unit> = object : PlayerDataAccess<Unit> {
            override fun getData(player: Player) {
            }

            override fun setData(player: Player, data: Unit) {
            }

            override fun players(): Set<Player> = emptySet()

            override fun unload(external: Boolean) {
            }
        }

        fun <T> adaptPlayerData(playerData: OnlinePlayerData<T>) : PlayerDataAccess<T> =
            object : PlayerDataAccess<T> {
                override fun getData(player: Player) = playerData[player]

                override fun setData(player: Player, data: T) {
                    playerData[player] = data
                }

                override fun players(): Set<Player> = playerData.keys.toSet()

                override fun unload(external: Boolean) {
                    playerData.unload(external)
                }
            }
    }
}