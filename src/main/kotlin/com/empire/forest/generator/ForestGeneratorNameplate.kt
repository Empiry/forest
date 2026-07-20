package com.empire.forest.generator

import com.empire.forest.ForestContext
import com.empire.hacks.common.builder.ClientEntityMetadataHelpers
import com.empire.ignite.util.GlobalResourceTrackers
import com.empire.ignite.util.HACKS
import com.empire.ignite.util.IgniteResource
import com.empire.ignite.util.UnloadableResource
import com.empire.ignite.util.collection.OnlinePlayerData
import com.empire.ignite.util.entity.ClientEntity
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class ForestGeneratorNameplate(
    plugin: JavaPlugin,
    context: ForestContext,
    private val description: GeneratorDescription,
    private val generator: ForestGenerator
) : UnloadableResource {
    private val clientEntity = ClientEntity.textDisplay(description.place.toVector())
    private val taskMap : OnlinePlayerData<ForestGeneratorNameplateUpdateTask> = OnlinePlayerData(
        plugin,
        onEvict = {_, task: ForestGeneratorNameplateUpdateTask ->
            task.unload()
        }
    )

    init {
        context.locateGeneratorsCallbackRegistration.register { (p, b) ->
            if (b) {
                onAdd(p)
            } else {
                onRemove(p)
            }
        }
    }

    fun onAdd(p: Player) {
        val task = ForestGeneratorNameplateUpdateTask(
            p,
            description, generator,
            clientEntity
        )
        task.load()
        taskMap[p] = task
    }

    fun onRemove(p: Player) {
        taskMap.remove(p)?.unload()
    }

    override fun unload(external: Boolean) {
        taskMap.unload()
    }
}

class ForestGeneratorNameplateUpdateTask(
    private val p: Player,
    private val desc: GeneratorDescription,
    private val generator: ForestGenerator,
    private val clientEntity: ClientEntity
) : IgniteResource {
    private var task: UnloadableResource? = null

    override fun load() {
        clientEntity.spawn(p)
        onEmit()
        task = GlobalResourceTrackers.scheduler.repeatTask(5L, ::onEmit)
    }

    private fun onEmit() {
        val rawDistance = desc.place.toLocation(p.world)!!.distance(p.location)
        val distance = "%.1f".format(rawDistance)
        val placeName = desc.name
        val progress = floor(
            (generator.progressTicks.toDouble() / (desc.unlockSeconds * 20)) * 100.0
        ).toInt()
        val displayText = Component.text("${distance}m").color(NamedTextColor.LIGHT_PURPLE).append(
            Component.text(" | ").color(NamedTextColor.DARK_GRAY)
        ).append(
            Component.text("$placeName ").color(NamedTextColor.GOLD)
        ).append(
            Component.text("${progress}%").color(
                if (progress < 100) NamedTextColor.YELLOW else NamedTextColor.GREEN
            )
        )
        val scalingFactor = max(1.75, min(16.0, rawDistance / 4)).toFloat()
        val metadata = listOf(
            ClientEntityMetadataHelpers.textDisplayText(displayText),
            ClientEntityMetadataHelpers.textDisplayOpacity(0x7F),
            ClientEntityMetadataHelpers.displayViewRange(10f),
            ClientEntityMetadataHelpers.displayOptionsScale(
                Vector(scalingFactor, scalingFactor, scalingFactor)
            ),
            ClientEntityMetadataHelpers.displayBillboard(HACKS, Display.Billboard.CENTER),
            ClientEntityMetadataHelpers.textDisplayOptionsMask(
                HACKS,
                shadow = false, seeThrough = true, defaultBgColor = false,
                alignment = TextDisplay.TextAlignment.CENTER
            ),
        )
        clientEntity.metadataUpdate(p, metadata)
    }

    override fun unload(external: Boolean) {
        clientEntity.destroy(p)
        task?.unload()
        task = null
    }
}