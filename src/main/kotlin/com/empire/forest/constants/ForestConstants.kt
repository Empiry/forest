package com.empire.forest.constants

import com.empire.forest.ForestApplication
import com.empire.forest.util.ForestMessaging
import com.empire.ignite.util.PluginResources
import com.empire.ignite.util.amountSuffix
import com.empire.ignite.util.config.InteractiveConfigV2
import com.empire.ignite.util.config.InteractiveConfigV2Schema
import com.empire.ignite.util.item.ItemBuilder
import com.empire.ignite.util.text.TextUtils.flattenComponents
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.TooltipDisplay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment

object ForestConstants {
    val FOREST_COLOR = TextColor.color(158, 26, 11)
    val SURVIVORS_COLOR = TextColor.color(207, 103, 19)
    val HUNTERS_COLOR = TextColor.color(194, 21, 21)
    val SPECTATORS_COLOR = TextColor.color(66, 135, 245)
    val GENERIC_QUEUE_END_TAG = "queue_end"

    private val MAP_CONFIGURATION_SCHEMA = InteractiveConfigV2Schema.NamedStructure(mutableMapOf(
        "name" to InteractiveConfigV2Schema.TextData,
        "generators" to
            InteractiveConfigV2Schema.TypedArray(
                InteractiveConfigV2Schema.NamedStructure(mutableMapOf(
                    "name" to InteractiveConfigV2Schema.AdventureComponent,
                    "region" to InteractiveConfigV2Schema.RegionData
                ))
            )
    ))

    val CONFIG_DATA = PluginResources.createManagedStore(
InteractiveConfigV2Schema.NamedStructure(
            mutableMapOf(
                "isDebug" to InteractiveConfigV2Schema.BooleanData,
                "mapConfigs" to InteractiveConfigV2Schema.TypedArray(MAP_CONFIGURATION_SCHEMA),
            )
        ),
    "forest",
    InteractiveConfigV2.NamedStructure(
            mutableMapOf(
                "isDebug" to InteractiveConfigV2.BooleanData(false),
                "mapConfigs" to InteractiveConfigV2.ArrayData(
                    MAP_CONFIGURATION_SCHEMA,
                    mutableListOf()
                ),
            )
        )
    )

    val HUNTER_MASK = ItemBuilder(Material.ACACIA_BUTTON) {
        name(Component.text("Wendigo Mask").color(HUNTERS_COLOR))
        enchantments(listOf(Enchantment.BINDING_CURSE to 1))
        itemModel(NamespacedKey("horror", "hats/hunter_mask"))
        dataComponent(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false)
        hideStuff()
    }


    fun getQueueMessageFormatter(countdownTicks: Long) : (Long) -> Component {
        return {ticks: Long ->
            if (ticks == 0L)
                ForestMessaging.withPrefix(
                    flattenComponents(
                        Component.text("Game is now ").color(NamedTextColor.GRAY),
                        Component.text("starting").color(NamedTextColor.GOLD),
                    )
                )
            else {
                var commandInfoAppend : Component = Component.empty()
                if (ticks >= countdownTicks) {
                    commandInfoAppend =
                        flattenComponents(
                            Component.text(" Do ").color(NamedTextColor.GRAY),
                            Component.text("/q")
                                .color(NamedTextColor.GREEN)
                                .decorate(TextDecoration.UNDERLINED),
                            Component.text(" to ").color(NamedTextColor.GRAY),
                            Component.text("queue!").color(NamedTextColor.GREEN),
                        )
                }
                val seconds = ticks / 20
                ForestMessaging.withPrefix(
                    flattenComponents(
                        Component.text("Game starting in ").color(NamedTextColor.GRAY),
                        Component.text(
                            "$seconds ${amountSuffix("second", seconds.toInt())}!"
                        ).color(NamedTextColor.GOLD),
                        commandInfoAppend
                    )
                )
            }
        }
    }
}