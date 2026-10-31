package com.empire.forest.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import kotlin.math.max
import kotlin.math.min

@CommandAlias("menudebug")
@CommandPermission("ignite.menudebug")
class MenuDebugCommand : BaseCommand() {
    @Default
    fun menudebug(
        player: Player, @Default("3") rows: Int = 3, @Default("\"im dum\"") title: String
    ) {
        val slots = min(max(1, rows), 6) * 9
        val testTitle = GsonComponentSerializer.gson().deserialize(title);
//        player.sendMessage(testTitle)
        val inv = Bukkit.createInventory(
            null, slots, testTitle.color(NamedTextColor.WHITE)
        )
        player.openInventory(inv)
    }
}