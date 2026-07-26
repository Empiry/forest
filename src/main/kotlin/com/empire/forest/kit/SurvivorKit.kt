package com.empire.forest.kit

import com.empire.forest.ForestContext
import com.empire.forest.mechanic.survivalist.TallGrassInvisibility
import com.empire.ignite.game.kit.GameKit
import com.empire.ignite.game.kit.ItemKitComponent
import com.empire.ignite.util.InventoryUtils
import com.empire.ignite.util.item.ItemBuilder
import com.empire.ignite.util.text.TextUtils.flattenComponents
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material

enum class SurvivorKit(val guiItem: ItemBuilder, val gameKit: GameKit<ForestContext>) {
    NAVIGATOR(ItemBuilder(Material.COMPASS) {
        name(Component.text("Navigator").color(NamedTextColor.LIGHT_PURPLE))

        lore(
            InventoryUtils.postprocessLore(
                listOf(
                    Component.text("The default kit!").color(NamedTextColor.GRAY),
                    flattenComponents(
                        Component.text("Hold the ").color(NamedTextColor.GRAY),
                        Component.text("compass ").color(NamedTextColor.RED),
                        Component.text("to ").color(NamedTextColor.RED),
                        Component.text("discover ").color(NamedTextColor.YELLOW),
                        Component.text("generators!").color(NamedTextColor.GRAY),
                    )
                )
            )
        )

        unbreakable()
    }, GameKit() { player, context ->
        com.empire.ignite.game.kit.IgniteBundle(
            itemComponent = ItemKitComponent(
                mapOf(
                    0 to context.locateGeneratorsItemBuilder.build()
                )
            )
        )
    }),
    SURVIVALIST(ItemBuilder(Material.TALL_GRASS) {
        name(Component.text("Survivalist!").color(NamedTextColor.GREEN))

        lore(
            InventoryUtils.postprocessLore(
                listOf(
                    Component.text("You can hide in tall grass while sneaking").color(NamedTextColor.GRAY)
                )
            )
        )

        unbreakable()
    }, GameKit() { player, context ->
        com.empire.ignite.game.kit.IgniteBundle(
            abilityComponents = listOf(
                com.empire.ignite.game.kit.AbilityKitComponent(
                    TallGrassInvisibility(player, context)
                )
            )
        )
    }),
    TRAPPER(ItemBuilder(Material.SHEARS) {
        name(Component.text("Trapper").color(NamedTextColor.RED))

        lore(
            InventoryUtils.postprocessLore(
                listOf(
                    flattenComponents(
                        Component.text("Place down ").color(NamedTextColor.GRAY),
                        Component.text("bear traps ").color(NamedTextColor.YELLOW),
                        Component.text("to stop the ").color(NamedTextColor.GRAY),
                        Component.text("hunter ").color(NamedTextColor.RED),
                    ),
                    Component.text("in their tracks").color(NamedTextColor.GRAY),
                    Component.text("(8 bear traps)").color(NamedTextColor.GRAY),
                )
            )
        )

        unbreakable()
    }, GameKit() { player, context ->
        val bearTrapItem = context.forestMechanics.getBearTrapItem(player)
        bearTrapItem.amount = 8
        com.empire.ignite.game.kit.IgniteBundle(
            ItemKitComponent(
                listOf(
                    0 to bearTrapItem
                ).toMap(),
            )
        )
    });
}