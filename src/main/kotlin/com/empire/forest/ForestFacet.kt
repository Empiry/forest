package com.empire.forest

import com.empire.forest.generator.ForestGenerator
import com.empire.ignite.Ignite
import com.empire.ignite.game.application.GameFacetV2
import com.empire.ignite.util.GlobalResourceTrackers
import com.empire.ignite.util.IGNITE_LOGGER
import com.empire.ignite.util.UnloadableResource
import com.empire.ignite.util.location.RawLocation
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.SoundCategory

class ForestFacet(
    plugin: Ignite,
    context: ForestContext
) : GameFacetV2<ForestStaticData, ForestContext>(plugin, context) {
    private val dump = GlobalResourceTrackers.createResourceDump()

    override fun onLoad() {
        val hunter = context.playerTracker.players.random()
        context.playerTracker.players.forEach { player ->
            if (player == hunter) {
                context.hunterTeam.addPlayer(player)
            } else {
                context.survivorTeam.addPlayer(player)
            }
        }

        val success = context.loadSpawns()
        if (!success) {
            this.panic("World was not loaded")
            return
        }
        context.hunterTeam.players.forEach { player -> player.teleport(context.huntersSpawn) }
        context.survivorTeam.players.forEach { player -> player.teleport(context.survivorsSpawn) }
        dump.add(object : UnloadableResource {
            override fun unload(external: Boolean) {
                context.hunterTeam.unload()
                context.survivorTeam.unload()
            }
        })

        val world = Bukkit.getWorld(context.staticData.worldName)
        listOf(
            GeneratorDescription(
                "Dock",
                RawLocation(-24.0, -56.0, 36.0),
                12
            )
        ).forEach { desc ->
            val spot = desc.place.toLocation(world)!!
            val generator = ForestGenerator(plugin, context, spot, desc.unlockSeconds) {
                context.playerTracker.players.forEach { survivor ->
                    survivor.sendMessage(
                        Component.text("Generator ", NamedTextColor.GRAY)
                            .append(Component.text(desc.name, NamedTextColor.GOLD))
                            .append(Component.text(" was completed!", NamedTextColor.GRAY))
                    )
                    survivor.playSound(
                        survivor.location, Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0F, 1.0F
                    )
                }
                checkGateOpen()
            }
            dump.add(generator)
            generator.load()
        }
    }

    private fun checkGateOpen() {

    }

    override fun onUnload() {
        dump.destroyAll()
    }
}

private class GeneratorDescription(
    val name: String,
    val place: RawLocation,
    val unlockSeconds: Int
)