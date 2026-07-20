package com.empire.forest.resourcepack

import com.empire.ignite.util.item.ItemBuilder
import org.bukkit.Material

/**
 * These are resource-pack constants
 * Thanks Ryan!
 */
object ResourcePackConstants {
    val BEAR_TRAP_OPEN_ITEM_CUSTOMMODELDATA = 1
    val BEAR_TRAP_OPEN_ITEM = ItemBuilder(Material.SHEARS) {
        customModelData(BEAR_TRAP_OPEN_ITEM_CUSTOMMODELDATA)
    }
}