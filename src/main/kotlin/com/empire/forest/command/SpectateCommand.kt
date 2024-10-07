package com.empire.forest.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import com.empire.ignite.player.IgnitePlayerTracker
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

@CommandAlias("spectate")
class SpectateCommand : BaseCommand() {
    @Default
    fun spectate(player: Player) {
        if (IgnitePlayerTracker.isInMatch(player)) {
            player.sendMessage(Component.text("Cannot spectate a match while in one"))
            return
        }
        IgnitePlayerTracker.getProfile(player)?.match?.context?.playerTracker?.addSpectator(player)
    }
}
