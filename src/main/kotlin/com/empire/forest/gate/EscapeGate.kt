package com.empire.forest.gate

import com.empire.forest.ForestContext
import com.empire.ignite.util.IgniteResource
import com.empire.ignite.util.location.RawLocation
import com.empire.ignite.util.region.Region
import com.empire.ignite.util.region.RegionPlayerTracker
import com.empire.ignite.util.region.RegionPlayerTrackerCallback
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.plugin.java.JavaPlugin

class EscapeGate(
    plugin: JavaPlugin,
    private val context: ForestContext,
    region: Region,
    private val escapeCallback: (Player) -> Unit
) : IgniteResource, RegionPlayerTrackerCallback {
    private val tracker = RegionPlayerTracker(plugin, region)

    override fun load() {
        tracker.load()
        tracker.callbacks.add(this)
    }

    override fun onPlayerEnter(movementEvent: Cancellable, player: Player) {
        if (player !in context.survivorTeam.players) return
        escapeCallback(player)
    }

    override fun unload(external: Boolean) {
        tracker.callbacks.remove(this)
        tracker.unload()
    }
}

data class EscapeGateDescription(
    val region: Region
)