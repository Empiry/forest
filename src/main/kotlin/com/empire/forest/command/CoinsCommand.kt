package com.empire.forest.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import com.empire.forest.data.ForestUserData
import com.empire.forest.data.ForestUserDatabase
import com.empire.ignite.util.sendMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

@CommandAlias("coins")
class CoinsCommand(val userDatabase: ForestUserDatabase) : BaseCommand() {
    @Default
    fun coins(player: Player) {
        val coins = userDatabase.access.get(player.uniqueId)?.coins ?: 0
        sendMessage(player,
            Component.text("You have ").color(NamedTextColor.YELLOW),
            Component.text(coins).color(NamedTextColor.GREEN),
            Component.text(" coin${if (coins == 1) "" else "s"}").color(NamedTextColor.YELLOW),
        )
    }

    @Subcommand("set")
    @CommandPermission("forest.coins.set")
    private fun set(player: Player, coins: Int) {
        userDatabase.access.setCached(player.uniqueId, ForestUserData(coins))
    }
}