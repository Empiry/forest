package com.empire.forest.generator

import com.empire.forest.ForestContext
import com.empire.ignite.Ignite
import com.empire.ignite.util.IgniteResource
import com.empire.ignite.util.entity.EntityModelBuilder
import com.empire.ignite.util.location.RawLocation
import com.empire.ignite.util.text.TextUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.math.floor

class ForestGenerator(
    private val ignite: Ignite,
    private val forestContext: ForestContext,
    private val location: Location,
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
    private var modelEntity: Entity? = null
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
        modelEntity = GENERATOR_DISPLAY.spawn(location)
    }

    private val workingPlayers : MutableList<Player> = mutableListOf()
    private fun process() {
        if (completed) return
        var activeContributors = 0
        for (player in forestContext.playerTracker.players) {
            if (player.location.world != this.location.world) continue
            if (player.location.distanceSquared(this.location) <= 4) {
                workingPlayers += player
                if (player.isSneaking) {
                    if (forestContext.isSurvivor(player))
                        activeContributors++
                    else if (forestContext.isHunter(player))
                        activeContributors--
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

        progressTicks += toAdd
        progressTicks = progressTicks.coerceAtLeast(0)
        val completion = ((progressTicks.toDouble() / (unlockTicks)) * 100).toInt()
        if (activeContributors != 0) onProgress(completion)
        val message = TextUtils.createProgressText("|", completion, 15)
        workingPlayers.forEach { workingPlayer ->
            if (workingPlayer.isSneaking) workingPlayer.sendActionBar(message)
            else
                workingPlayer.sendActionBar(
                    if (forestContext.isSurvivor(workingPlayer))
                        Component.text(
                        "Sneak to activate generator...", NamedTextColor.GRAY, TextDecoration.ITALIC
                        )
                    else
                        Component.text(
                        "Sneak to de-activate generator...", NamedTextColor.GRAY, TextDecoration.ITALIC
                        )
                )
        }
        workingPlayers.clear()
        if (progressTicks >= unlockTicks) {
            completed = true
            unload()
            onComplete()
        }
    }

    override fun unload(external: Boolean) {
        if (tickerTask != -1) Bukkit.getScheduler().cancelTask(tickerTask)
        tickerTask = -1
        modelEntity?.remove()
        modelEntity = null
    }
}

class GeneratorDescription(
    val name: String,
    val place: RawLocation,
    val unlockSeconds: Int
)