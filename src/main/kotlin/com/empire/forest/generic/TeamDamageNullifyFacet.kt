package com.empire.forest.generic

import com.empire.forest.ForestContext
import com.empire.forest.ForestStaticData
import com.empire.ignite.Ignite
import com.empire.ignite.game.application.GameFacetV2
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent

// not sure why friendlyFire isn't working
class TeamDamageNullifyFacet(
    plugin: Ignite,
    context: ForestContext,
) : GameFacetV2<ForestStaticData, ForestContext>(plugin, context), Listener {
    @EventHandler
    private fun onDamage(e: EntityDamageByEntityEvent) {
        val entity = e.entity
        val damager = e.damager
        if (entity is Player && damager is Player && context.playerTracker.isPlaying(e.entity as Player)) {
            e.isCancelled = (context.findPlayerTeam(entity) == context.findPlayerTeam(damager))
        }
    }
}