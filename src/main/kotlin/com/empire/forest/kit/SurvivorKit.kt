package com.empire.forest.kit

import com.empire.forest.ForestContext
import com.empire.forest.mechanic.survivalist.TallGrassInvisibility
import com.empire.forest.perk.BearTrapConsumeConfig
import com.empire.forest.perk.BearTrapPerk
import com.empire.forest.perk.BearTrapPlayerData
import com.empire.forest.perk.MedicPerk
import com.empire.forest.perk.PerkAbilityAdapter
import com.empire.ignite.game.kit.GameKit
import com.empire.ignite.game.kit.ItemKitComponent
import com.empire.ignite.util.InventoryUtils
import com.empire.ignite.util.item.ItemBuilder
import com.empire.ignite.util.text.TextUtils.flattenComponents
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey

enum class SurvivorKit(val guiItem: ItemBuilder, val gameKit: GameKit<ForestContext>) {
    NAVIGATOR(ItemBuilder(Material.COMPASS) {
        name(Component.text("Navigator").decoration(TextDecoration.ITALIC, false))

        lore(
            InventoryUtils.postprocessLore(
                listOf(
                    Component.text("ꜱᴜʀᴠɪᴠᴏʀ ᴋɪᴛ").color(NamedTextColor.DARK_GRAY),
                    Component.text(""),
                    flattenComponents(
                        Component.text(" | ").color(NamedTextColor.DARK_GRAY),
                        Component.text("\"Spawns with a ").color(NamedTextColor.GRAY),
                        Component.text("Compass").color(TextColor.fromHexString("#FFE100")),
                    ),
                    flattenComponents(
                        Component.text(" | ").color(NamedTextColor.DARK_GRAY),
                        Component.text("that shows all ").color(NamedTextColor.GRAY),
                        Component.text("Generators").color(TextColor.fromHexString("#0083FF")),
                    ),
                    flattenComponents(
                        Component.text(" | ").color(NamedTextColor.DARK_GRAY),
                        Component.text("and their percentages.\"").color(NamedTextColor.GRAY),
                    ),
                    Component.text("")
                )
            )
        )

        unbreakable()
        hideStuff()
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
        name(Component.text("Survivalist").decoration(TextDecoration.ITALIC, false))
        itemModel(NamespacedKey("horror", "perks/stealthy"))

        lore(
            InventoryUtils.postprocessLore(
                listOf(
                    Component.text("ꜱᴜʀᴠɪᴠᴏʀ ᴋɪᴛ").color(NamedTextColor.DARK_GRAY),
                    Component.text(""),
                    flattenComponents(
                        Component.text(" | ").color(NamedTextColor.DARK_GRAY),
                        Component.text("\"Allows you to go ").color(NamedTextColor.GRAY),
                        Component.text("Invisible").color(TextColor.fromHexString("#8000FF")),
                        Component.text(" by").color(NamedTextColor.GRAY),
                    ),
                    flattenComponents(
                        Component.text(" | ").color(NamedTextColor.DARK_GRAY),
                        Component.text("crouching for 2s in Tall Foliage.\"").color(NamedTextColor.GRAY),
                    ),
                    Component.text("")
                )
            )
        )

        unbreakable()
        hideStuff()
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
                    Component.text("(4 bear traps)").color(NamedTextColor.GRAY),
                )
            )
        )

        unbreakable()
    }, GameKit() { player, context ->
        com.empire.ignite.game.kit.IgniteBundle(
            abilityComponents = listOf(
                com.empire.ignite.game.kit.AbilityKitComponent(
                    PerkAbilityAdapter(
                        BearTrapPerk::class.java,
                        context, player
                    ) { BearTrapPlayerData(
                        trapCount = 4, consumeConfig = BearTrapConsumeConfig.Limited(4)) }
                )
            )
        )
    }),
    MEDIC(ItemBuilder(Material.STICK) {
        itemModel(NamespacedKey("horror", "perks/bottle_of_pills"))
        name(Component.text("Medic").decoration(TextDecoration.ITALIC, false))

        lore(
            InventoryUtils.postprocessLore(
                listOf(
                    Component.text("ꜱᴜʀᴠɪᴠᴏʀ ᴋɪᴛ").color(NamedTextColor.DARK_GRAY),
                    Component.text(""),
                    flattenComponents(
                        Component.text(" | ").color(NamedTextColor.DARK_GRAY),
                        Component.text("\"Spawns with 2x ").color(NamedTextColor.GRAY),
                        Component.text("Bottle of Pills").color(TextColor.fromHexString("#0083FF")),
                        Component.text(".\"").color(NamedTextColor.GRAY),
                    ),
                    Component.text("")
                )
            )
        )

        unbreakable()
        hideStuff()
    }, GameKit() { player, context ->
        com.empire.ignite.game.kit.IgniteBundle(
            abilityComponents = listOf(
                com.empire.ignite.game.kit.AbilityKitComponent(
                    PerkAbilityAdapter(
                        MedicPerk::class.java,
                        context, player
                    ) { }
                )
            )
        )
    });
}