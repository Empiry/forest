package com.empire.forest.constants

import com.empire.ignite.util.PluginResources
import com.empire.ignite.util.config.InteractiveConfigV2
import com.empire.ignite.util.config.InteractiveConfigV2Schema
import net.kyori.adventure.text.format.TextColor

object ForestConstants {
    val FOREST_COLOR = TextColor.color(59, 179, 91)
    val SURVIVORS_COLOR = TextColor.color(207, 103, 19)
    val CONFIG_DATA = PluginResources.createManagedStore(
InteractiveConfigV2Schema.NamedStructure(
            mutableMapOf(
                "isDebug" to InteractiveConfigV2Schema.BooleanData
            )
        ),
    "forest",
    InteractiveConfigV2.NamedStructure(
            mutableMapOf("isDebug" to InteractiveConfigV2.BooleanData(false))
        )
    )
}