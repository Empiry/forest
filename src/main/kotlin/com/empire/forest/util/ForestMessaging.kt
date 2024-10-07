package com.empire.forest.util

import com.empire.forest.constants.ForestConstants
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

object ForestMessaging {
    fun withPrefix(component: Component) : Component {
        return Component.text("TheForest").color(ForestConstants.FOREST_COLOR)
            .append(Component.text(" » ").color(NamedTextColor.DARK_GRAY))
            .append(component)
    }

    fun createForestMessage(message: String) : Component {
        return withPrefix(Component.text(message).color(NamedTextColor.GRAY))
    }

    private val GENERATOR_NOT_STARTED_COLOR = TextColor.color(128, 125, 120)
    private val GENERATOR_IN_PROGRESS_COLOR = NamedTextColor.YELLOW
    private val GENERATOR_COMPLETE_COLOR = NamedTextColor.GREEN
    fun getGeneratorProgressColor(generatorProgress: Int) : TextColor {
        return when (generatorProgress) {
            0 -> GENERATOR_NOT_STARTED_COLOR
            100 -> GENERATOR_COMPLETE_COLOR
            else -> GENERATOR_IN_PROGRESS_COLOR
        }
    }
}