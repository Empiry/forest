package com.empire.forest.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import com.empire.ignite.player.IgnitePlayerTracker
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

@CommandAlias("leave")
class LeaveCommand : BaseCommand() {
    @Default
    fun leave(player: Player) {
        if (!IgnitePlayerTracker.isInMatch(player)) {
            player.sendMessage(Component.text("You are not in a match!"))
            return
        }
        IgnitePlayerTracker.getProfile(player)?.match?.context?.playerTracker?.removePlayer(player)
    }
}