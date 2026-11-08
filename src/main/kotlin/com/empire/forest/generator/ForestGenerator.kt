package com.empire.forest.generator

import com.empire.forest.ForestContext
import com.empire.forest.perk.ComponentShape
import com.empire.forest.util.ForestMessaging
import com.empire.hacks.common.builder.ClientEntityMetadataHelpers
import com.empire.ignite.Ignite
import com.empire.ignite.util.GlobalResourceTrackers
import com.empire.ignite.util.HACKS
import com.empire.ignite.util.IgniteResource
import com.empire.ignite.util.UnloadableResourcePostHook
import com.empire.ignite.util.entity.ClientEntity
import com.empire.ignite.util.entity.EntityModelBuilder
import com.empire.ignite.util.entity.ManagedEntity
import com.empire.ignite.util.entity.ManagedEntityOptions
import com.empire.ignite.util.location.RawLocation
import com.empire.ignite.util.region.CuboidRegion
import com.empire.ignite.util.region.Region
import com.empire.ignite.util.text.TextUtils
import com.empire.ignite.util.text.TextUtils.flattenComponents
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Display
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import java.util.*
import kotlin.math.floor

class ForestGenerator(
    private val ignite: Ignite,
    private val forestContext: ForestContext,
    private val description: GeneratorDescription,
    val location: Location,
    unlockSeconds: Int,
    private val contributionMultiplier: (Int) -> Double = { n -> n.toDouble() },
    private val onProgress: (Int) -> Unit,
    private val onComplete: () -> Unit,
) : IgniteResource, Listener {
    var completed : Boolean = false
        private set
    private var tickerTask : Int = -1
    private val unlockTicks = unlockSeconds*20
    private val random = Random()
    private var modelEntity: ItemDisplay? = null
    private val captureRegion : Region
    private val halfwayPoint = unlockTicks / 2
    private var indicator : GeneratorWarningIndicator? = null

    init {
        captureRegion = CuboidRegion(
            location.clone()
                .add(description.getLengthDir().clone().multiply(-3.5))
                .add(Vector(0, -3, 0))
                .add(description.getWidthDir().clone().multiply(-2.5)).toVector(),
            location.clone()
                .add(description.getLengthDir().clone().multiply(3.5))
                .add(description.getWidthDir().clone().multiply(2.5)).toVector()
//            Vector(location.x + 2.5, location.y - 3, location.z + 1.5),
//            Vector(location.x - 2.5, location.y, location.z - 2.5)
        )

        if (forestContext.staticData.debugLever && false) {
            if (description.name.lowercase() == "cellar") {
                Bukkit.broadcast(Component.text(
                    "---"
                ))
                Bukkit.broadcast(Component.text(
                    "setting debug blocks at generator location ${description.name}", NamedTextColor.YELLOW
                ))
                Bukkit.broadcast(Component.text(
                    "base location ${location}", NamedTextColor.YELLOW
                ))
                Bukkit.broadcast(Component.text(
                    "length vector ${description.getLengthDir()}", NamedTextColor.YELLOW
                ))
                Bukkit.broadcast(Component.text(
                    "width vector ${description.getWidthDir()}", NamedTextColor.YELLOW
                ))
                Bukkit.broadcast(Component.text(
                    "captureRegion ${captureRegion}"
                ))
                Bukkit.broadcast(Component.text(
                    "---"
                ))
            }
            val a =
                location.clone()
                    .add(description.getLengthDir().clone().multiply(-3.5))
                    .add(Vector(0, -3, 0))
                    .add(description.getWidthDir().clone().multiply(-2.5))
            val b = location.clone()
                .add(description.getLengthDir().clone().multiply(3.5))
                .add(description.getWidthDir().clone().multiply(2.5))
            location.block.type = Material.SPONGE
            a.block.type = Material.DIAMOND_BLOCK
            b.block.type = Material.EMERALD_BLOCK
        }
    }

    companion object {
        private val GENERATOR_DISPLAY : EntityModelBuilder<ItemDisplay>

        init {
            val itemModel = ItemStack(Material.STONE)
            val meta = itemModel.itemMeta;
            meta.itemModel = NamespacedKey("horror", "objects/big_generator_off");
            itemModel.setItemMeta(meta);
            GENERATOR_DISPLAY = EntityModelBuilder(ItemDisplay::class.java) {
                onSpawn {
                    this.setItemStack(itemModel)
                }
            }
        }
    }

    var progressTicks : Int = 0
        private set

    override fun load() {
        tickerTask = Bukkit.getScheduler().runTaskTimer(ignite, this::process, 0L, 1L).taskId
        if (modelEntity == null) {
            modelEntity = GENERATOR_DISPLAY.spawn(location)
            modelEntity?.setRotation(description.rotation.toFloat(), 0.0f)
        }
    }

    private val workingPlayers : MutableList<Player> = mutableListOf()
    private fun process() {
        if (completed) return
        var activeContributors = 0
        var activeDetractors = 0
        for (player in forestContext.playerTracker.players) {
            if (player.location.world != this.location.world) continue
//            val inRegion = player.location.distanceSquared(this.location) <= 4
            val inRegion = captureRegion.contains(player.location.toVector())
            if (inRegion) {
                workingPlayers += player
                if (player.isSneaking) {
                    if (forestContext.isSurvivor(player))
                        activeContributors++
                    else if (forestContext.isHunter(player))
                        activeDetractors++
                }
            }
        }
        var toAdd = 0
        if (activeContributors > 0) {
            val multiplier = contributionMultiplier(activeContributors)
            toAdd = activeContributors
            val integerPart = floor(multiplier).toInt()
            val fractionalPart = (multiplier - integerPart)
            toAdd *= integerPart
            if (random.nextDouble() < fractionalPart) {
                toAdd += 1
            }
        }
        if (activeDetractors > 0) {
            val multiplier = 1.6
            val integerPart = floor(multiplier).toInt()
            val fractionalPart = (multiplier - integerPart)
            toAdd -= integerPart
            if (random.nextDouble() < fractionalPart) {
                toAdd -= 1
            }
        }

        progressTicks += toAdd
        if (toAdd > 0 && progressTicks == halfwayPoint) {
            forestContext.sendToHunters(
                ForestMessaging.withPrefix(
                    flattenComponents(
                        Component.text(description.name).color(NamedTextColor.YELLOW),
                        Component.text(" is now at ").color(NamedTextColor.GRAY),
                        Component.text("50%").color(NamedTextColor.RED),
                    )
                )
            )
            if (indicator == null) {
                indicator = GeneratorWarningIndicator(
                    forestContext, location.clone().add(0.0, 0.4, 0.0)
                )
            }
        }
        if (progressTicks < halfwayPoint && indicator != null) {
            indicator?.unloadWithPostHook()
            indicator = null
        }
        progressTicks = progressTicks.coerceAtLeast(0)
        val completion = ((progressTicks.toDouble() / (unlockTicks)) * 100).toInt()
        if (activeContributors != 0) onProgress(completion)
        val message =
            forestContext.jamIfNeeded(
                TextUtils.createProgressText("|", completion, 15),
                ComponentShape(20, NamedTextColor.DARK_GRAY)
            )

        workingPlayers.forEach { workingPlayer ->
            if (workingPlayer.isSneaking) workingPlayer.sendActionBar(message)
            else
                workingPlayer.sendActionBar(
                    if (forestContext.isSurvivor(workingPlayer))
                        forestContext.jamIfNeeded(
                            Component.text(
                                "Sneak to activate generator...", NamedTextColor.GRAY, TextDecoration.ITALIC
                            ),
                            ComponentShape(30, NamedTextColor.GRAY)
                        )
                    else
                        forestContext.jamIfNeeded(
                        Component.text(
                        "Sneak to de-activate generator...", NamedTextColor.GRAY, TextDecoration.ITALIC
                        ),
                    ComponentShape(30, NamedTextColor.GRAY)
                        )
                )
        }
        workingPlayers.clear()
        if (progressTicks >= unlockTicks) {
            completed = true
            unload()
            turnOnGenerator()
            onComplete()
            if (indicator != null) {
                indicator?.unloadWithPostHook()
                indicator = null
            }
        }
    }

    override fun unload(external: Boolean) {
        if (tickerTask != -1) Bukkit.getScheduler().cancelTask(tickerTask)
        tickerTask = -1
    }

    private fun turnOnGenerator() {
        val itemModel = ItemStack(Material.STONE)
        val meta = itemModel.itemMeta;
        meta.itemModel = NamespacedKey("horror", "objects/big_generator_on");
        itemModel.setItemMeta(meta);
        modelEntity?.setItemStack(itemModel)
    }
}

class GeneratorDescription(
    val name: String,
    val place: RawLocation,
    val unlockSeconds: Int,
    val rotation: Double = 0.0
) {
    companion object {
        val LENGTH_DIR = Vector(-1, 0, 0)
        val WIDTH_DIR = Vector(0, 0, -1)
    }

    fun getLengthDir() : Vector =
        LENGTH_DIR.clone().rotateAroundY(Math.toRadians(rotation))

    fun getWidthDir() : Vector =
        WIDTH_DIR.clone().rotateAroundY(Math.toRadians(rotation))
}

class GeneratorWarningIndicator(
    context: ForestContext,
    location: Location
) : UnloadableResourcePostHook {
    private val hooks : MutableList<() -> Unit> = mutableListOf()
    private val dump = GlobalResourceTrackers.createResourceDump()

    init {
        val me = ManagedEntity.textDisplay(
            location.toVector(),
            ClientEntity.textDisplay(context.world!!, location.toVector()),
            options = setOf(ManagedEntityOptions.DYNAMIC_SCALING)
        )
        context.hunterTeam.players.forEach { chud ->
            me.apply(chud)
            me.metadataUpdate(
                chud,
                listOf(
                    ClientEntityMetadataHelpers.textDisplayText(Component.text("\uE009")),
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
        dump.add(me)
    }

    override fun unloadWithPostHook() {
        dump.destroyAll()
        hooks.forEach { it() }
    }

    override fun addPostHook(hook: () -> Unit) {
        hooks += hook
    }
}