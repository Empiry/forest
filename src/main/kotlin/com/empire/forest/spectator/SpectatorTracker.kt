package com.empire.forest.spectator

import com.empire.forest.ForestContext
import com.empire.ignite.game.application.SpectateMatchEvent
import com.empire.ignite.game.application.UnspectateMatchEvent
import com.empire.ignite.util.UnloadableResource
import com.empire.ignite.util.registerListener
import com.empire.ignite.util.unregisterListener
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

class SpectatorTracker(
    plugin: JavaPlugin,
    private val context: ForestContext
) : UnloadableResource, Listener {
    init {
        registerListener(plugin, this)
    }

    @EventHandler
    private fun onSpectate(event: SpectateMatchEvent) {
        if (context.match != event.match) return
        context.spectatorsChangeSetContract.addSink(event.player)
    }

    @EventHandler
    private fun onUnspectate(event: UnspectateMatchEvent) {
        if (context.match != event.match) return
        context.spectatorsChangeSetContract.removeSink(event.player)
    }

    override fun unload(external: Boolean) {
        unregisterListener(this)
    }
}