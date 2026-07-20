package com.empire.forest.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import com.empire.forest.constants.ForestConstants.CONFIG_DATA
import com.empire.ignite.Ignite
import com.empire.ignite.util.PluginResources
import com.empire.ignite.util.config.*
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

@CommandAlias("testicv")
@CommandPermission("ignite.admin")
class TestIcvCommand(
    plugin: Ignite
): BaseCommand() {
    private val icvEditor: InteractiveConfigV2Editor = InteractiveConfigV2Editor(plugin)

    @Default
    fun testICV(player: Player) {
        icvEditor.startEditSession(
            player,
            CONFIG_DATA.store,
            object : EditRequesterCallback {
                override fun onSessionComplete() {
                    player.sendMessage(
                        Component.text("Saved ICV!")
                    )
                }
            }
        )
    }
}