package com.empire.forest.perk

import com.empire.forest.ForestContext
import com.empire.forest.constants.ForestConstants
import com.empire.forest.util.PlayerDataAccess
import com.empire.ignite.display.DisplayUpdater
import com.empire.ignite.display.Displays
import com.empire.ignite.util.*
import com.empire.ignite.util.collection.OnlinePlayerData
import com.empire.ignite.util.collection.OnlinePlayerSet
import com.empire.ignite.util.item.ItemBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector

class BuckshotPerk(
    plugin: JavaPlugin,
    private val context: ForestContext
) : ForestPerk<Unit>, Listener {
    companion object {
        private val crossbowIdentifier = NamespacedKey("chud", "buckshot")
        private val crossbowIB = ItemBuilder(Material.CROSSBOW) {
            name(Component.text("Buckshot Shotgun").color(ForestConstants.HUNTERS_COLOR))
            pdcBool(crossbowIdentifier, true)
            itemModel(NamespacedKey("horror", "weapons/buckshot"))
            lore(
                listOf(
                    Component.text("Right-click to shoot")
                        .color(NamedTextColor.RED)
                )
            )
        }

        private val arrowIdentifier = NamespacedKey("chud", "arrow")
        val arrowIB = ItemBuilder(Material.ARROW) {
            pdcBool(arrowIdentifier, true)
        }

        val AMMO_INIT = 2
    }

    private val players = OnlinePlayerSet()
    private val playerData = OnlinePlayerData<BuckshotPlayerData>(plugin, onEvict = { _, rel -> rel.unload() })
    private var task : Int = -1
    override val contextManager: PlayerDataAccess<Unit> = PlayerDataAccess.noop()

    init {
        registerListener(plugin, this)
        players.load()
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, 2L).taskId
    }

    override fun apply(player: Player) {
        player.give(crossbowIB.build())
        player.inventory.setItem(17, arrowIB.build())
        val noiseCancelUnloadable = UnloadableResource.fromHacks(
            HACKS.cancelNoiseStrategy(listOf(player), "item.crossbow")
        )
        val data = BuckshotPlayerData(player, ammo = AMMO_INIT)
        data.unloadables += noiseCancelUnloadable
        playerData += (player to data)
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

    private fun getShotRays(playerLoc: Location): List<Ray> {
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

        val spread = Math.toRadians(20.0)

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

        val rays = directions.map { direction ->
            val raySpot = spot.clone()
            raySpot.direction = direction.clone()
            val ray = Ray(raySpot, distance = 12.0, stepSize = 0.25)
            ray
        }
        return rays
    }

    @EventHandler
    private fun onCrossbowInteract(event: PlayerInteractEvent) {
        if (event.item == null) return
        if (event.player !in players) return
        val player = event.player
        val item = player.inventory.getItem(player.activeItemHand)
        if (item.type != Material.CROSSBOW) return
        val data = playerData[player] ?: return
        if (data.ammo > 0) return
        data.outOfAmmo()
        player.playSound(
            player.location, "minecraft:buckshot_jammed",
            SoundCategory.MASTER, 1.0f, 1.0f
        )
        event.isCancelled = true
    }

    @EventHandler
    private fun onBowShoot(event: EntityShootBowEvent) {
        if (event.entity !in players) return
        val data = playerData[event.entity] ?: return
        val player = (event.entity as Player)
        data.ammo--
        if (data.ammo > 0) player.inventory.setItem(17, arrowIB.build())
        player.playSound(
            player.location, "minecraft:buckshot_fire",
            SoundCategory.MASTER, 1.0f, 1.0f
        )
        val rays = getShotRays(player.location)
        val hit = rays.mapNotNull { ray ->
            val result = ray.cast(player)
            result.entity
        }
        hit.forEach {
            if (it !is Player) return@forEach
            if (!context.canDamage(player, it)) return@forEach
            it.damage(9.5, player)
        }
        rays.flatMap { it.getLocations() }.forEach {
            it.world.spawnParticle(
                Particle.SMOKE, it, 4,
                0.0, 0.0, 0.0,
                0.04
            )
        }
        event.isCancelled = true
    }

    @EventHandler
    private fun onDrop(event: PlayerDropItemEvent) {
        if (event.player !in players) return
        val arrowPDC =
            event.itemDrop.itemStack.persistentDataContainer.get(
                arrowIdentifier, PersistentDataType.BOOLEAN
            ) ?: false
        if (arrowPDC) {
            event.isCancelled = true
            return
        }
        val shotgunPDC =
            event.itemDrop.itemStack.persistentDataContainer.get(
                crossbowIdentifier, PersistentDataType.BOOLEAN
            ) ?: false
        if (shotgunPDC) {
            event.isCancelled = true
            val data = playerData[event.player] ?: return
            data.reloadAmmo()
            return
        }
    }

    override fun remove(player: Player) {
        players -= player
        playerData.remove(player)?.unload()
    }

    override fun unload(external: Boolean) {
        unregisterListener(this)
        players.unload()
        playerData.unload()
        if (task != -1) {
            Bukkit.getScheduler().cancelTask(task)
            task = -1
        }
    }
}

class BuckshotPlayerData(
    val player: Player,
    var ammo: Int,
    val unloadables: MutableList<UnloadableResource> = mutableListOf()
) : UnloadableResource {
    private val itemCheckID : Int
    private var titleID : Long = -1L
    private var reloading = false

    companion object {
        private val UPDATE_TICKS : Int = 2
    }

    init {
        itemCheckID = Bukkit.getScheduler().runTaskTimer(GlobalResourceTrackers.plugin!!, Runnable {
            val item = player.inventory.getItem(player.activeItemHand)
            if (item.type == Material.CROSSBOW && titleID == -1L) {
                enableSubtitle()
            } else if (item.type != Material.CROSSBOW && titleID != -1L) {
                disableSubtitle()
            }
        }, 0L , 2L).taskId
    }

    fun enableSubtitle() {
        if (titleID != -1L) return

        titleID = Displays.getSubtitle(player).displayQueueManager.submitTask(
            DisplayUpdater.createTitleUpdater(
                player,
                {
                    if (ammo == 0) null
                    else Title.title(
                        Component.empty(),
                        Component.text(
                            "\uE001".repeat(ammo)
                        ),
                        0, UPDATE_TICKS + 2, 0
                    )
                },
                UPDATE_TICKS.toLong()
            )
        )
    }

    fun outOfAmmo() {
        Displays.getSubtitle(player).displayQueueManager.submitTemporary(2L) {
            Title.title(
                Component.text("Out of ammo!").color(NamedTextColor.RED),
                Component.text("Drop to reload")
                    .color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC),
                0, 30, 0
            )
        }
    }

    fun reloadAmmo() {
        if (ammo >= BuckshotPerk.AMMO_INIT || reloading) return
        var updateTicks = 0
        freeze()
        Displays.getSubtitle(player).displayQueueManager.submitTemporary(50L) {
            updateTicks++
            val dots = ".".repeat(((updateTicks / 8) % 3) + 1)
            Title.title(
                Component.empty(),
                Component.text("Reloading${dots}")
                    .color(NamedTextColor.DARK_GRAY).decorate(TextDecoration.ITALIC),
                0, 30, 0
            )
        }
        Bukkit.getScheduler().runTaskLater(GlobalResourceTrackers.plugin!!, Runnable {
            ammo = BuckshotPerk.AMMO_INIT
            player.inventory.setItem(17, BuckshotPerk.arrowIB.build())
            unfreeze()
            reloading = false
        }, 50L)
        player.playSound(
            player.location, "minecraft:buckshot_reload",
            SoundCategory.MASTER, 1.0f, 1.0f
        )
        reloading = true
    }

    fun freeze() {
        player.getAttribute(Attribute.MOVEMENT_SPEED)?.let { it.baseValue = 0.0 }
        player.getAttribute(Attribute.JUMP_STRENGTH)?.let { it.baseValue = 0.0 }
    }

    fun unfreeze() {
        player.getAttribute(Attribute.MOVEMENT_SPEED)?.let { it.baseValue = 0.10000000149011612 }
        player.getAttribute(Attribute.JUMP_STRENGTH)?.let { it.baseValue = it.defaultValue }
    }

    fun disableSubtitle() {
        if (titleID == -1L) return
        Displays.getSubtitle(player).displayQueueManager.removeByID(titleID)
        titleID = -1L
    }

    override fun unload(external: Boolean) {
        disableSubtitle()
        Bukkit.getScheduler().cancelTask(itemCheckID)
        unloadables.forEach { it.unload() }
    }
}