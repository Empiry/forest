package com.empire.forest.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import com.empire.forest.ForestContext
import com.empire.forest.ForestStaticData
import com.empire.ignite.Ignite
import com.empire.ignite.game.facets.SpectatorFacet
import com.empire.ignite.match.MatchV2
import com.empire.ignite.player.IgnitePlayerTracker
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

@CommandAlias("spectate")
class SpectateCommand(
    private val plugin: Ignite
) : BaseCommand() {
    @Default
    fun spectate(player: Player) {
        if (IgnitePlayerTracker.isInMatch(player)) {
            player.sendMessage(Component.text("Cannot spectate a match while in one"))
            return
        }
        val match = plugin.matchManager.matches.mapNotNull {
            if (it.context !is ForestContext) return@mapNotNull null
            it.context.messageBus.findNodeByClass<SpectatorFacet<*, *>>() ?: return@mapNotNull null
            it
        }.firstOrNull()
        if (match == null) {
            player.sendMessage(
                Component.text("There are no matches to spectate").color(
                    NamedTextColor.RED
                )
            )
            return
        }
        val result = match.context.playerTracker.addSpectator(player)
        if (!result) {
            player.sendMessage(
                Component.text("You cannot spectate this match").color(
                    NamedTextColor.RED
                )
            )
            return
        }
    }
}
