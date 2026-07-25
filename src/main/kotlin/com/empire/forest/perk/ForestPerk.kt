package com.empire.forest.perk

import com.empire.forest.ForestContext
import com.empire.ignite.game.kit.IgniteAbilityV2
import com.empire.ignite.util.UnloadableResource
import org.bukkit.entity.Player

interface ForestPerk : UnloadableResource {
    fun apply(player: Player)
    fun remove(player: Player)
}

class PerkAbilityAdapter(
    perkClass: Class<out ForestPerk>,
    context: ForestContext,
    override val player: Player
) : IgniteAbilityV2 {
    private val perk: ForestPerk? = context.perks.firstOrNull { perkClass.isAssignableFrom(it.javaClass) }

    override fun enable() {
        perk?.apply(player)
    }

    override fun disable() {
        perk?.remove(player)
    }
}