package com.empire.forest.kit

import com.empire.forest.ForestContext
import com.empire.forest.mechanic.phantom.CloakingPerk
import com.empire.forest.mechanic.survivalist.TallGrassInvisibility
import com.empire.forest.perk.PerkAbilityAdapter
import com.empire.ignite.game.kit.GameKit
import com.empire.ignite.game.kit.IgniteBundle
import com.empire.ignite.game.kit.ItemKitComponent
import com.empire.ignite.util.InventoryUtils
import com.empire.ignite.util.item.ItemBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material

enum class HunterKit(val guiItem: ItemBuilder, val gameKit: GameKit<ForestContext>) {
    SLASHER(ItemBuilder(Material.SHEARS) {
        name(Component.text("Slasher").color(NamedTextColor.DARK_RED))

        lore(
            InventoryUtils.postprocessLore(
                listOf(
                    Component.text("The default kit!").color(NamedTextColor.GRAY),
                    Component.text("Includes 5 bear traps").color(NamedTextColor.RED),
                    Component.text(
                        "Drop them to use, survivors who walk into them may experience pain"
                    ).color(NamedTextColor.YELLOW),
                    Component.text(
                        "Careful! Stepping in them will self-inflict pain"
                    ).color(NamedTextColor.RED)
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
                ).toMap(),
            ),
            abilityComponents = listOf(
                com.empire.ignite.game.kit.AbilityKitComponent(
                    TallGrassInvisibility(player, context)
                )
            )
        )
    }),
    PHANTOM(ItemBuilder(Material.WIND_CHARGE) {
        name(Component.text("Phantom").color(NamedTextColor.LIGHT_PURPLE))

        lore(
            InventoryUtils.postprocessLore(
                listOf(
                    Component.text("4 cloaking potions that").color(NamedTextColor.YELLOW),
                    Component.text("help you sneak up on survivors").color(NamedTextColor.YELLOW),
                )
            )
        )

        unbreakable()
    }, GameKit() { player, context ->
        IgniteBundle(
            abilityComponents = listOf(
                com.empire.ignite.game.kit.AbilityKitComponent(
                    TallGrassInvisibility(player, context)
                ),
                com.empire.ignite.game.kit.AbilityKitComponent(
                    PerkAbilityAdapter(
                        CloakingPerk::class.java,
                        context,
                        player
                    )
                )
            )
        )
    });
}