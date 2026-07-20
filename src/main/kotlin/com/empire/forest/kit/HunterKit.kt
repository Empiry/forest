package com.empire.forest.kit

import com.empire.forest.ForestContext
import com.empire.ignite.game.kit.GameKit
import com.empire.ignite.game.kit.IgniteBundle
import com.empire.ignite.game.kit.ItemKitComponent
import com.empire.ignite.util.InventoryUtils
import com.empire.ignite.util.item.ItemBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material

enum class HunterKit(val guiItem: ItemBuilder, val gameKit: GameKit<ForestContext>) {
    HUNTER(ItemBuilder(Material.SHEARS) {
        name(Component.text("Hunter!").color(NamedTextColor.RED))

        lore(
            InventoryUtils.postprocessLore(
                listOf(
                    Component.text("The default kit!").color(NamedTextColor.GRAY),
                    Component.text("Includes 5 bear traps").color(NamedTextColor.RED),
                    Component.text(
                        "Drop them to use, survivors who walk into them may experience pain"
                    ).color(NamedTextColor.YELLOW)
                )
            )
        )

        unbreakable()
    }, GameKit() { player, context ->
        val bearTrapItem = context.forestMechanics.getBearTrapItem(player)
        bearTrapItem.amount = 10
        IgniteBundle(
            ItemKitComponent(
                listOf(
                    0 to bearTrapItem
                ).toMap()
            )
        )
    });
}