package com.empire.forest.gate

import com.empire.forest.ForestContext
import com.empire.hacks.common.builder.ClientEntityMetadataHelpers
import com.empire.ignite.util.GlobalResourceTrackers
import com.empire.ignite.util.HACKS
import com.empire.ignite.util.IgniteResource
import com.empire.ignite.util.entity.ClientEntity
import com.empire.ignite.util.entity.ManagedEntity
import com.empire.ignite.util.entity.ManagedEntityOptions
import com.empire.ignite.util.region.*
import net.kyori.adventure.text.Component
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.Cancellable
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector

class EscapeGate(
    plugin: JavaPlugin,
    private val context: ForestContext,
    private val region: Region,
    private val escapeCallback: (Player) -> Unit
) : IgniteResource, RegionPlayerTrackerCallback {
    private val tracker = RegionPlayerTracker(plugin, region)
    private val dump = GlobalResourceTrackers.createResourceDump()

    override fun load() {
        tracker.load()
        tracker.callbacks.add(this)
        center(region).forEach { loc ->
            val me = ManagedEntity.textDisplay(
                loc, ClientEntity.textDisplay(loc),
                options = setOf(ManagedEntityOptions.DYNAMIC_SCALING)
            )
            for (player in context.survivorTeam.players) {
                me.apply(player)
                me.metadataUpdate(
                    player,
                    listOf(
                        ClientEntityMetadataHelpers.textDisplayText(Component.text("\uE005")),
                        ClientEntityMetadataHelpers.textDisplayOpacity(0x7F),
                        ClientEntityMetadataHelpers.textDisplayBackground(0),
                        ClientEntityMetadataHelpers.displayViewRange(10f),
                        ClientEntityMetadataHelpers.displayBillboard(HACKS, Display.Billboard.CENTER),
                        ClientEntityMetadataHelpers.textDisplayOptionsMask(
                            HACKS,
                            shadow = false, seeThrough = true, defaultBgColor = false,
                            alignment = TextDisplay.TextAlignment.CENTER
                        )
                    )
                )
            }
            var flipState = false
            me.attachUnloadable(GlobalResourceTrackers.scheduler.repeatTask(30L) {
                flipState = !flipState
                val text = if (flipState) "\uE005" else "\uE006"
                for (player in context.survivorTeam.players) {
                    me.metadataUpdate(
                        player,
                        listOf(
                            ClientEntityMetadataHelpers.textDisplayText(Component.text(text)),
                        )
                    )
                }
            })
            dump.add(me)
        }
    }

    override fun onPlayerEnter(movementEvent: Cancellable, player: Player) {
        if (player !in context.survivorTeam.players) return
        escapeCallback(player)
    }

    override fun unload(external: Boolean) {
        tracker.callbacks.remove(this)
        tracker.unload()
        dump.destroyAll()
    }
}

data class EscapeGateDescription(
    val region: Region
)

private fun center(region: Region) : List<Vector> =
    when (region) {
        is AggregateRegion -> {
            region.regions.flatMap { center(it) }
        }
        is CuboidRegion -> listOf(region.center())
    }