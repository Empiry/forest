package com.empire.forest.generator

import com.empire.forest.ForestContext
import com.empire.forest.perk.ComponentShape
import com.empire.hacks.common.builder.ClientEntityMetadataHelpers
import com.empire.ignite.util.GlobalResourceTrackers
import com.empire.ignite.util.HACKS
import com.empire.ignite.util.IgniteResource
import com.empire.ignite.util.UnloadableResource
import com.empire.ignite.util.collection.OnlinePlayerData
import com.empire.ignite.util.entity.ClientEntity
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
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
    private val generatorDiscovery: ForestGeneratorDiscovery,
    private val context: ForestContext,
    private val description: GeneratorDescription,
    val generator: ForestGenerator
) : UnloadableResource {
    private val clientEntity = ClientEntity.textDisplay(
        description.place.toVector().add(Vector(0.0, 1.0, 0.0))
    )
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

    fun onAdd(p: Player, seeThru: Boolean = true) {
        if (p in taskMap) return
        val task = ForestGeneratorNameplateUpdateTask(
            p, context,  generatorDiscovery,
            description, generator,
            clientEntity, NameplateOptions(seeThru)
        )
        task.load()
        taskMap[p] = task
    }


    fun onRemove(p: Player) {
        if (p !in taskMap) return
        taskMap.remove(p)?.unload()
    }

    override fun unload(external: Boolean) {
        taskMap.unload()
    }
}

data class NameplateOptions(val seeThru: Boolean = true)
class ForestGeneratorNameplateUpdateTask(
    private val p: Player,
    private val context: ForestContext,
    private val generatorDiscovery: ForestGeneratorDiscovery,
    private val desc: GeneratorDescription,
    private val generator: ForestGenerator,
    private val clientEntity: ClientEntity,
    private val options: NameplateOptions = NameplateOptions()
) : IgniteResource {
    private var task: UnloadableResource? = null

    override fun load() {
        clientEntity.spawn(p)
        onEmit()
        task = GlobalResourceTrackers.scheduler.repeatTask(5L, ::onEmit)
    }

    private fun onEmit() {
        val rawDistance = desc.place.toLocation(p.world)!!.distance(p.location)
        val scalingFactor = max(1.75, min(16.0, rawDistance / 4)).toFloat()
        val genMetadata = { displayText: Component ->
            listOf(
                ClientEntityMetadataHelpers.textDisplayText(displayText),
                ClientEntityMetadataHelpers.textDisplayOpacity(0x7F),
                ClientEntityMetadataHelpers.displayViewRange(10f),
                ClientEntityMetadataHelpers.displayOptionsScale(
                    Vector(scalingFactor, scalingFactor, scalingFactor)
                ),
                ClientEntityMetadataHelpers.displayBillboard(HACKS, Display.Billboard.CENTER),
                ClientEntityMetadataHelpers.textDisplayOptionsMask(
                    HACKS,
                    shadow = false, seeThrough = options.seeThru, defaultBgColor = false,
                    alignment = TextDisplay.TextAlignment.CENTER
                ),
            )
        }

        val metadata = if (!generatorDiscovery.unlocked(desc)) {
            genMetadata(
                context.jamIfNeeded(
                    Component.text("[???]").color(NamedTextColor.DARK_GRAY)
                        .decorate(TextDecoration.BOLD),
                    ComponentShape(5, NamedTextColor.DARK_GRAY)
                )
            )
        } else {
            val distance = "%.1f".format(rawDistance)
            val placeName = desc.name
            val progress = floor(
                (generator.progressTicks.toDouble() / (desc.unlockSeconds * 20)) * 100.0
            ).toInt()
            val displayText = Component.text("${distance}m").color(NamedTextColor.RED).append(
                Component.text(" | ").color(NamedTextColor.DARK_GRAY)
            ).append(
                Component.text("$placeName ").color(TextColor.color(222, 16, 26))
            ).append(
                Component.text("${progress}%").color(
                    if (progress < 100) NamedTextColor.YELLOW else NamedTextColor.GREEN
                )
            )
            genMetadata(
                context.jamIfNeeded(
            displayText,
                    ComponentShape(20, TextColor.color(222, 16, 26))
                )
            )
        }
        clientEntity.metadataUpdate(p, metadata)
    }

    override fun unload(external: Boolean) {
        clientEntity.destroy(p)
        task?.unload()
        task = null
    }
}


class NameplateProximityTask(
    plugin: JavaPlugin,
    private val context: ForestContext,
    private val generators: List<ForestGenerator>
) : Runnable, UnloadableResource {
    val overrides: OnlinePlayerData<Boolean> = OnlinePlayerData(plugin)

    override fun run() {
        val nameplateMap = context.nameplates.associateBy { it -> it.generator }
        generators.forEach { generator ->
            val nameplate = nameplateMap[generator] ?: return@forEach
            context.playerAccess.alive.filter {
                it !in overrides || !(overrides[it] ?: false)
            }.forEach { player ->
                val dstSq = generator.location.distanceSquared(player.location)
                if (dstSq <= 64.0) nameplate.onAdd(player, false)
                else nameplate.onRemove(player)
            }
        }
    }

    override fun unload(external: Boolean) {
        overrides.unload(external)
    }
}