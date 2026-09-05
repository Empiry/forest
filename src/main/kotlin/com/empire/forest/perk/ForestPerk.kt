package com.empire.forest.perk

import com.empire.forest.ForestContext
import com.empire.forest.util.PlayerDataAccess
import com.empire.ignite.game.kit.IgniteAbilityV2
import com.empire.ignite.util.UnloadableResource
import org.bukkit.entity.Player

interface ForestPerk<C> : UnloadableResource {
    val contextManager: PlayerDataAccess<C>
    fun apply(player: Player)
    fun remove(player: Player)
}

class PerkAbilityAdapter<C>(
    perkClass: Class<out ForestPerk<C>>,
    context: ForestContext,
    override val player: Player,
    private val pc: () -> C
) : IgniteAbilityV2 {
    private val perk: ForestPerk<C>? = context.perks.firstOrNull {
        perkClass.isAssignableFrom(it.javaClass)
    } as ForestPerk<C>?

    override fun enable() {
        perk?.contextManager?.setData(player, pc())
        perk?.apply(player)
    }

    override fun disable() {
        perk?.remove(player)
    }
}