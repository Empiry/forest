package com.empire.forest.resourcepack

import com.empire.ignite.util.item.ItemBuilder
import org.bukkit.Material
import org.bukkit.NamespacedKey

/**
 * These are resource-pack constants
 * Thanks Ryan!
 */
object ResourcePackConstants {
    val BEAR_TRAP_OPEN_ITEM_CUSTOMMODELDATA = 1
    val BEAR_TRAP_OPEN_ITEM = ItemBuilder(Material.SHEARS) {
        customModelData(BEAR_TRAP_OPEN_ITEM_CUSTOMMODELDATA)
    }

    val BEAR_TRAP_OPEN_ITEM_SURVIVOR = ItemBuilder(Material.STONE) {
        itemModel(NamespacedKey("horror", "objects/survivor_beartrap"))
    }

    val BEAR_TRAP_OPEN_ITEM_HUNTER = ItemBuilder(Material.STONE) {
        itemModel(NamespacedKey("horror", "objects/hunter_beartrap"))
    }

    val BEAR_TRAP_CLOSED_ITEM = ItemBuilder(Material.STONE) {
        itemModel(NamespacedKey("horror", "objects/closed_beartrap"))
    }
}