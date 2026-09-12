package com.empire.forest.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Subcommand
import com.empire.ignite.display.Displays
import org.bukkit.entity.Player

@CommandAlias("forestdebug")
@CommandPermission("ignite.admin")
class ForestDebugCommand : BaseCommand() {
    @Subcommand("actionbar")
    private fun onActionBarDebug(player: Player) {
        player.sendMessage("actionbar instance: ${Displays.getActionBar(player)}")
        player.sendMessage("loaded? ${Displays.getActionBar(player).displayQueueManager.displayQueue.loaded}")
        player.sendMessage("entries? ${Displays.getActionBar(player).displayQueueManager.displayQueue.entries}")
        player.sendMessage("taskID? ${Displays.getActionBar(player).displayQueueManager.displayQueue.taskID}")
    }
}