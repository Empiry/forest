package com.empire.forest.perk

import com.empire.forest.ForestContext
import com.empire.forest.constants.ForestConstants
import com.empire.hacks.common.HacksResourceRelease
import com.empire.ignite.util.HACKS
import com.empire.ignite.util.collection.OnlinePlayerData
import com.empire.ignite.util.collection.OnlinePlayerSet
import com.empire.ignite.util.getLocationsInPath
import com.empire.ignite.util.item.ItemBuilder
import com.empire.ignite.util.registerListener
import com.empire.ignite.util.unregisterListener
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDropItemEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector

class BuckshotPerk(
    plugin: JavaPlugin,
    private val context: ForestContext
) : ForestPerk, Listener {
    companion object {
        private val crossbowIB = ItemBuilder(Material.CROSSBOW) {
            name(Component.text("Buckshot Shotgun").color(ForestConstants.HUNTERS_COLOR))
            itemModel(NamespacedKey("horror", "weapons/buckshot"))
            lore(
                listOf(
                    Component.text("Right-click to shoot")
                        .color(NamedTextColor.RED)
                )
            )
        }

        private val chudIdentifier = NamespacedKey("chud", "arrow")
        private val chudArrowIB = ItemBuilder(Material.ARROW) {
            pdcBool(chudIdentifier, true)
        }
    }

    private val players = OnlinePlayerSet()
    private val cancellables = OnlinePlayerData<HacksResourceRelease>(plugin, onEvict = {_, rel -> rel.release() })
    private var task : Int = -1

    init {
        registerListener(plugin, this)
        players.load()
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, 2L).taskId
    }

    override fun apply(player: Player) {
        player.give(crossbowIB.build())
        player.inventory.setItem(17, chudArrowIB.build())
        cancellables[player] = HACKS.cancelNoiseStrategy(listOf(player), "item.crossbow")
        players += player
    }

    private fun tick() {
        for (player in players) {
            if (player.activeItem.type == Material.CROSSBOW) {
                if (player.activeItemUsedTime in 1..2) {
                    player.playSound(
                        player.location, "minecraft:buckshot_reload",
                        SoundCategory.MASTER, 1.0f, 1.0f
                    )
                }
            }

        }
    }

    private fun getShotLocations(playerLoc: Location): List<Location> {
        val forward = playerLoc.direction.clone().normalize()

        val spot = playerLoc.clone()
            .add(0.0, 1.0, 0.0)
            .add(forward.clone().multiply(0.7))

        val worldUp = Vector(0, 1, 0)

        val right = forward.clone()
            .crossProduct(worldUp)
            .normalize()

        val up = right.clone()
            .crossProduct(forward)
            .normalize()

        val spread = Math.toRadians(30.0)

        val directions = listOf(
            // Center
            forward.clone(),

            // Left / right
            forward.clone().rotateAroundAxis(up, spread),
            forward.clone().rotateAroundAxis(up, -spread),

            // Up
            forward.clone().rotateAroundAxis(right, spread),

            // Up-left / up-right
            forward.clone()
                .rotateAroundAxis(right, spread)
                .rotateAroundAxis(up, spread),

            forward.clone()
                .rotateAroundAxis(right, spread)
                .rotateAroundAxis(up, -spread),

            // Down
            forward.clone().rotateAroundAxis(right, -spread),

            // Down-left / down-right
            forward.clone()
                .rotateAroundAxis(right, -spread)
                .rotateAroundAxis(up, spread),

            forward.clone()
                .rotateAroundAxis(right, -spread)
                .rotateAroundAxis(up, -spread),
        )

        return directions.flatMap {
            val iterator = getLocationsInPath(
                spot,
                it.normalize(),
                stepSize = 0.25
            )

            List(14) { iterator.next() }
        }
    }

    @EventHandler
    private fun onBowShoot(event: EntityShootBowEvent) {
        if (event.entity !in players) return
        val player = (event.entity as Player)
        player.inventory.setItem(17, chudArrowIB.build())
        player.playSound(
            player.location, "minecraft:buckshot_fire",
            SoundCategory.MASTER, 1.0f, 1.0f
        )
        getShotLocations(player.location).forEach {
            it.world.spawnParticle(
                Particle.SMOKE, it, 4,
                0.0, 0.0, 0.0,
                0.04
            )
        }
        event.isCancelled = true
    }

    @EventHandler
    private fun onDrop(event: EntityDropItemEvent) {
        if (event.entity !in players) return
        if (!(
            event.itemDrop.itemStack.persistentDataContainer.get(
                chudIdentifier, PersistentDataType.BOOLEAN
            ) ?: false
        )) return
        event.isCancelled = true
    }

    override fun remove(player: Player) {
        players -= player
        cancellables.remove(player)?.release()
    }

    override fun unload(external: Boolean) {
        unregisterListener(this)
        players.unload()
        cancellables.unload()
        if (task != -1) {
            Bukkit.getScheduler().cancelTask(task)
            task = -1
        }
    }
}