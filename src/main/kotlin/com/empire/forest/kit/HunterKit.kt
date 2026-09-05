package com.empire.forest.kit

import com.empire.forest.ForestContext
import com.empire.forest.mechanic.phantom.CloakingPerk
import com.empire.forest.mechanic.survivalist.TallGrassInvisibility
import com.empire.forest.mechanic.werewolf.WerewolfSpeedPerk
import com.empire.forest.perk.BearTrapPerk
import com.empire.forest.perk.BearTrapPlayerData
import com.empire.forest.perk.BuckshotPerk
import com.empire.forest.perk.MotionSensorPerk
import com.empire.forest.perk.NewMotionSensorPerk
import com.empire.forest.perk.PerkAbilityAdapter
import com.empire.forest.perk.RoarPerk
import com.empire.ignite.game.kit.GameKit
import com.empire.ignite.game.kit.IgniteBundle
import com.empire.ignite.game.kit.ItemKitComponent
import com.empire.ignite.util.InventoryUtils
import com.empire.ignite.util.item.ItemBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material

private val SWORD = ItemBuilder(Material.IRON_SWORD) {}.build()
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
        IgniteBundle(
            ItemKitComponent(
                listOf(
                    0 to SWORD
                ).toMap(),
            ),
            abilityComponents = listOf(
                com.empire.ignite.game.kit.AbilityKitComponent(
                    TallGrassInvisibility(player, context)
                ),
                com.empire.ignite.game.kit.AbilityKitComponent(
                    PerkAbilityAdapter(
                        RoarPerk::class.java,
                        context, player, { }
                    )
                ),
                com.empire.ignite.game.kit.AbilityKitComponent(
                    PerkAbilityAdapter(
                        BuckshotPerk::class.java,
                        context, player, { }
                    )
                ),
                com.empire.ignite.game.kit.AbilityKitComponent(
                    PerkAbilityAdapter(
                        BearTrapPerk::class.java,
                        context, player
                    ) { BearTrapPlayerData(trapCount = 8) }
                ),
                com.empire.ignite.game.kit.AbilityKitComponent(
                    PerkAbilityAdapter(
                        NewMotionSensorPerk::class.java,
                        context, player
                    ) { }
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
            ItemKitComponent(
                listOf(
                    0 to SWORD
                ).toMap(),
            ),
            abilityComponents = listOf(
                com.empire.ignite.game.kit.AbilityKitComponent(
                    TallGrassInvisibility(player, context)
                ),
                com.empire.ignite.game.kit.AbilityKitComponent(
                    PerkAbilityAdapter(
                        CloakingPerk::class.java,
                        context, player, { }
                    )
                )
            )
        )
    }),
    WEREWOLF(ItemBuilder(Material.BONE) {
        name(Component.text("Werewolf").color(TextColor.color(128, 66, 4)))

        lore(
            InventoryUtils.postprocessLore(
                listOf(
                    Component.text("3 speed boosts!").color(NamedTextColor.YELLOW)
                )
            )
        )

        unbreakable()
    }, GameKit() { player, context ->
        IgniteBundle(
            ItemKitComponent(
                listOf(
                    0 to SWORD
                ).toMap(),
            ),
            abilityComponents = listOf(
                com.empire.ignite.game.kit.AbilityKitComponent(
                    TallGrassInvisibility(player, context)
                ),
                com.empire.ignite.game.kit.AbilityKitComponent(
                    PerkAbilityAdapter(
                        WerewolfSpeedPerk::class.java,
                        context, player, { }
                    )
                )
            )
        )
    }),
    SCARECROW(ItemBuilder(Material.HAY_BLOCK) {
        name(Component.text("Scarecrow").color(NamedTextColor.GOLD))

        lore(
            InventoryUtils.postprocessLore(
                listOf(
                    Component.text("Place down a scarecrow to alert you").color(NamedTextColor.YELLOW),
                    Component.text("of nearby survivors").color(NamedTextColor.YELLOW),
                )
            )
        )

        unbreakable()
    }, GameKit() { player, context ->
        IgniteBundle(
            ItemKitComponent(
                listOf(
                    0 to SWORD
                ).toMap(),
            ),
            abilityComponents = listOf(
                com.empire.ignite.game.kit.AbilityKitComponent(
                    TallGrassInvisibility(player, context)
                ),
                com.empire.ignite.game.kit.AbilityKitComponent(
                    PerkAbilityAdapter(
                        MotionSensorPerk::class.java,
                        context, player, { }
                    )
                )
            )
        )
    });
}