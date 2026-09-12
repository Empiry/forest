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
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ChargedProjectiles
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector

class BuckshotPerk(
    plugin: JavaPlugin,
    private val context: ForestContext
) : ForestPerk<Unit>, Listener {
    companion object {
        private val crossbowIdentifier = NamespacedKey("chud", "buckshot")
        private val arrowIdentifier = NamespacedKey("chud", "arrow")
        val AMMO_INIT = 2
    }
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
    private val chargedIB : ItemBuilder
    val arrowIB = ItemBuilder(Material.ARROW) {
        pdcBool(arrowIdentifier, true)
    }
    private val shotgunCompare : (ItemStack) -> Boolean

    private val players = OnlinePlayerSet()
    private val playerData = OnlinePlayerData<BuckshotPlayerData>(plugin, onEvict = { _, rel -> rel.unload() })
    private var task : Int = -1
    override val contextManager: PlayerDataAccess<Unit> = PlayerDataAccess.noop()

    init {
        registerListener(plugin, this)
        players.load()
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, 2L).taskId
        val cloned = crossbowIB.clone()
        cloned.dataComponent(
            DataComponentTypes.CHARGED_PROJECTILES,
            ChargedProjectiles.chargedProjectiles(
                listOf(ItemStack(Material.ARROW))
            )
        )
        chargedIB = cloned
        val normalPredicate = itemCompare(plugin, crossbowIB)
        val chargedPredicate = itemCompare(plugin, chargedIB)
        shotgunCompare = { istack -> normalPredicate(istack) || chargedPredicate(istack) }
    }

    override fun apply(player: Player) {
        player.give(crossbowIB.build())
        val noiseCancelUnloadable = UnloadableResource.fromHacks(
            HACKS.cancelNoiseStrategy(listOf(player), "item.crossbow")
        )
        val data = BuckshotPlayerData(
            context, player, shotgunCompare,
            crossbowIB.build(), chargedIB.build(),
            ammo = AMMO_INIT
        )
        data.unloadables += noiseCancelUnloadable
        playerData += (player to data)
        players += player
        context.getForestPlayerContextData(player)?.setupArrowReplenishmentTask()
    }

    private fun tick() {
        for (player in players) {
            if (shotgunCompare(player.activeItem)) {
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
        if (!shotgunCompare(item)) return
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
        if (!shotgunCompare(player.inventory.itemInMainHand)) return
        data.ammo--
        player.playSound(
            player.location, "minecraft:buckshot_fire",
            SoundCategory.MASTER, 1.0f, 1.0f
        )
        val rays = getShotRays(player.location)
        val hit = rays.mapNotNull { ray ->
            val result = ray.cast(player)
            result.entity
        }
        val dmgMap = mutableMapOf<Entity, Int>()
        hit.forEach {
            val current = dmgMap[it] ?: 0
            dmgMap[it] = current + 1
        }
        dmgMap.entries.forEach { (k, v) ->
            if (k !is Player) return@forEach
            if (!context.canDamage(player, k)) return@forEach
            context.survivorHealth.syntheticDamagePlayer(player, k, 6.0f * v)
        }
        rays.flatMap { it.getLocations() }.forEach {
            it.world.spawnParticle(
                Particle.SMOKE, it, 4,
                0.0, 0.0, 0.0,
                0.04
            )
        }
        event.isCancelled = true
        if (data.ammo > 0 && !context.gameProbablyOver()) {
            player.inventory.setItem(EquipmentSlot.HAND, ItemStack(Material.AIR))
            player.inventory.setItem(EquipmentSlot.HAND, chargedIB.build())
        }
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
            data.reloadAmmo(arrowIB)
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
        playerData.values.forEach { it.unload(external) }
        playerData.unload()
        if (task != -1) {
            Bukkit.getScheduler().cancelTask(task)
            task = -1
        }
    }
}

class BuckshotPlayerData(
    private val context: ForestContext,
    val player: Player,
    val shotgunCompare: (ItemStack) -> Boolean,
    val shotgunItem: ItemStack, val chargedShotgunItem: ItemStack,
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
            if (shotgunCompare(item) && titleID == -1L) {
                enableSubtitle()
            } else if (!shotgunCompare(item) && titleID != -1L) {
                disableSubtitle()
            }
        }, 0L , 2L).taskId
    }

    fun enableSubtitle() {
        if (titleID != -1L) return

        titleID = Displays.getSubtitle(player).displayQueueManager.submitTask(
            DisplayUpdater.createTitleUpdater(
                player,
                upd@{
                    if (ammo == 0) null
                    else {
                        val ammoText = "\uE001".repeat(ammo) +
                                "\uE002".repeat(BuckshotPerk.AMMO_INIT - ammo)
                        return@upd Title.title(
                            Component.empty(),
                            Component.text(ammoText),
                            0, UPDATE_TICKS + 2, 0
                        )
                    }
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

    fun reloadAmmo(arrowIB: ItemBuilder) {
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
            player.inventory.all(chargedShotgunItem).keys.forEach {
                player.inventory.setItem(it, shotgunItem)
            }
            ammo = BuckshotPerk.AMMO_INIT
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
        player.getAttribute(Attribute.MOVEMENT_SPEED)?.let { it.baseValue = context.getNormalMovementSpeed(player) }
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