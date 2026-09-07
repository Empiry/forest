package com.empire.forest.generic

import com.empire.forest.ForestContext
import com.empire.forest.ForestStaticData
import com.empire.ignite.Ignite
import com.empire.ignite.game.application.GameFacetV2
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent

class TeamDamageNullifyFacet(
    plugin: Ignite,
    context: ForestContext,
) : GameFacetV2<ForestStaticData, ForestContext>(plugin, context), Listener {
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private fun onDamage(e: EntityDamageByEntityEvent) {
        val entity = e.entity
        val damager = e.damager
        if (entity is Player && damager is Player && context.playerTracker.isPlaying(e.entity as Player)) {
            if (context.findPlayerTeam(entity) == context.findPlayerTeam(damager)) {
                e.isCancelled = true
            }
        }
    }
}