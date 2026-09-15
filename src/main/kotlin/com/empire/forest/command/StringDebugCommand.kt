package com.empire.forest.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import org.bukkit.entity.Player

class StringDebugCommand(

) : BaseCommand() {
    @Default
    private fun onStringQuery(player: Player) {

    }

    @Subcommand("unlock")
    private fun onStringUnlock(player: Player) {

    }

    @Subcommand("lock")
    private fun onStringLock(player: Player) {

    }

    private fun getStringState(player: Player) {
//        player.getTargetBlock()

    }

    enum class StringState {
        LOCKED, UNLOCKED, NOT_STRING
    }
}