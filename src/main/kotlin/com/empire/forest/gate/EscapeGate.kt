package com.empire.forest.gate

import com.empire.forest.ForestContext
import com.empire.ignite.util.IgniteResource
import com.empire.ignite.util.location.RawLocation
import com.empire.ignite.util.registerListener
import com.empire.ignite.util.unregisterListener
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.java.JavaPlugin

class EscapeGate(
    private val plugin: JavaPlugin,
    private val context: ForestContext,
    private val escapeLocation: Location,
    radius: Int,
    private val escapeCallback: (Player) -> Unit
) : IgniteResource, Listener {
    private val distanceSquared = radius*radius

    override fun load() {
        registerListener(plugin, this)
    }

    @EventHandler
    private fun onMove(event: PlayerMoveEvent) {
        if (event.player !in context.survivorTeam.players) return
        if (event.to.distanceSquared(escapeLocation) > distanceSquared) return
        escapeCallback(event.player)
    }

    override fun unload(external: Boolean) {
        unregisterListener(this)
    }
}

data class EscapeGateDescription(val location: RawLocation, val radius: Int)