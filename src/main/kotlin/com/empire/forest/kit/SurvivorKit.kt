package com.empire.forest.kit

import com.empire.forest.ForestContext
import com.empire.forest.mechanic.survivalist.TallGrassInvisibility
import com.empire.ignite.game.kit.GameKit
import com.empire.ignite.game.kit.ItemKitComponent
import com.empire.ignite.util.InventoryUtils
import com.empire.ignite.util.item.ItemBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material

enum class SurvivorKit(val guiItem: ItemBuilder, val gameKit: GameKit<ForestContext>) {
    SURVIVALIST(ItemBuilder(Material.TALL_GRASS) {
        name(Component.text("Survivalist!").color(NamedTextColor.GREEN))

        lore(
            InventoryUtils.postprocessLore(
                listOf(
                    Component.text("The default kit!").color(NamedTextColor.GRAY),
                    Component.text("You have legs, use them!").color(NamedTextColor.GRAY),
                    Component.text(
                        "(You can also hide in tall grass while sneaking)"
                    ).color(NamedTextColor.GRAY)
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
            ),
            abilityComponents = listOf(
                com.empire.ignite.game.kit.AbilityKitComponent(
                    TallGrassInvisibility(player, context)
                )
            )
        )
    });
}