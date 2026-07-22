package com.empire.forest.perk

import com.empire.ignite.util.UnloadableResource
import org.bukkit.entity.Player

interface ForestPerk : UnloadableResource {
    fun apply(player: Player)
    fun remove(player: Player)
}