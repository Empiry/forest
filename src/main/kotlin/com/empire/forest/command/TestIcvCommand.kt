package com.empire.forest.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import com.empire.ignite.Ignite
import com.empire.ignite.util.config.EditRequesterCallback
import com.empire.ignite.util.config.ICVV2FileStore
import com.empire.ignite.util.config.InteractiveConfigV2Editor
import com.empire.ignite.util.config.InteractiveConfigV2Schema
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import java.io.File

@CommandAlias("testicv")
class TestIcvCommand(
    plugin: Ignite
): BaseCommand() {
    private val icvEditor: InteractiveConfigV2Editor = InteractiveConfigV2Editor(plugin)

    @Default
    fun testICV(player: Player) {
        icvEditor.startEditSession(
            player,
            ICVV2FileStore(
                InteractiveConfigV2Schema.NamedStructure(mutableMapOf(
                    "test1" to InteractiveConfigV2Schema.Text,
                    "test2" to InteractiveConfigV2Schema.Number,
                    "test3" to InteractiveConfigV2Schema.PlayerInventory,
                    "test4" to InteractiveConfigV2Schema.AdventureComponent,
                    "test5" to InteractiveConfigV2Schema.LocationWithWorld,
                    "test6" to InteractiveConfigV2Schema.LocationWithoutWorld
                )),
                File("test.icv")
            ),
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