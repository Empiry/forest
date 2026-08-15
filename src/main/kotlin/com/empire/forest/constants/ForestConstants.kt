package com.empire.forest.constants

import com.empire.ignite.util.PluginResources
import com.empire.ignite.util.config.InteractiveConfigV2
import com.empire.ignite.util.config.InteractiveConfigV2Schema
import net.kyori.adventure.text.format.TextColor

object ForestConstants {
    val FOREST_COLOR = TextColor.color(59, 179, 91)
    val SURVIVORS_COLOR = TextColor.color(207, 103, 19)
    val HUNTERS_COLOR = TextColor.color(194, 21, 21)

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
}