package com.empire.forest.kit

import com.empire.ignite.util.InventoryUtils
import com.empire.ignite.util.item.ItemBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material

enum class SurvivorKit(val guiItem: ItemBuilder) {
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
    });
}