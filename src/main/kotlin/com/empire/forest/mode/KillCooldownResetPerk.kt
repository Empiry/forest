package com.empire.forest.mode

import com.empire.forest.perk.ForestPerk
import com.empire.forest.util.PlayerDataAccess
import org.bukkit.entity.Player

class KillCooldownResetPerk : ForestPerk<Unit> {
    override val contextManager: PlayerDataAccess<Unit> = PlayerDataAccess.noop()

    override fun apply(player: Player) {
    }

    override fun remove(player: Player) {
    }

    override fun unload(external: Boolean) {
    }
}