package com.empire.forest.perk

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent
import com.empire.forest.ForestContext
import com.empire.forest.util.ItemDisplayUtils
import com.empire.forest.util.PlayerDataAccess
import com.empire.hacks.common.builder.ClientEntityMetadataHelpers
import com.empire.ignite.game.facets.DeathEvents
import com.empire.ignite.util.*
import com.empire.ignite.util.collection.OnlinePlayerData
import com.empire.ignite.util.entity.ClientEntity
import com.empire.ignite.util.entity.ManagedEntity
import com.empire.ignite.util.entity.ManagedEntityOptions
import com.empire.ignite.util.item.ItemBuilder
import com.empire.ignite.util.projectile.*
import io.papermc.paper.event.player.PlayerArmSwingEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.*
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class MachetePerk(
    private val plugin: JavaPlugin,
    private val context: ForestContext
) : ForestPerk<Unit>, Listener, DeathEvents {
    override val contextManager: PlayerDataAccess<Unit> = PlayerDataAccess.noop()
    private val macheteIb = ItemBuilder(Material.CARROT_ON_A_STICK) {
        name(Component.text("Machete").color(NamedTextColor.YELLOW))
        itemModel(NamespacedKey("horror", "weapons/machete"))
        unbreakable(true)
    }
    private val bloodyMacheteIb = ItemBuilder(Material.TRIDENT) {
        name(Component.text("Bloody Machete").color(NamedTextColor.RED))
        itemModel(NamespacedKey("horror", "weapons/machete_bloody"))
//        dataComponent(
//            DataComponentTypes.CHARGED_PROJECTILES,
//            ChargedProjectiles.chargedProjectiles(
//                listOf(ItemStack(Material.ARROW))
//            )
//        )
        unbreakable(true)
    }
    private val arrowIB = ItemBuilder(Material.ARROW) {
        unbreakable(true)
    }
    private val dump = GlobalResourceTrackers.createResourceDump()
    private val playerData : OnlinePlayerData<MachetePlayerData> = OnlinePlayerData(plugin)
    private val normalMacheteCompare : (ItemStack) -> Boolean
    private val bloodyMacheteCompare : (ItemStack) -> Boolean

    init {
        dump.add(denyItemDrop(plugin, macheteIb))
        dump.add(denyItemDrop(plugin, bloodyMacheteIb))
        dump.add(denyItemDrop(plugin, arrowIB))
        registerListener(plugin, this)
        normalMacheteCompare = itemCompare(plugin, macheteIb)
        bloodyMacheteCompare = itemCompare(plugin, bloodyMacheteIb)
    }

    override fun apply(player: Player) {
        player.give(bloodyMacheteIb.build())
        playerData += player to MachetePlayerData()
    }

    @EventHandler
    private fun onSwing(event: PlayerArmSwingEvent) {
        val data = playerData[event.player] ?: return
        val isNormal = normalMacheteCompare(event.player.inventory.itemInMainHand)
        val isBloody = bloodyMacheteCompare(event.player.inventory.itemInMainHand)
        if (isNormal || isBloody) {
            if (data.lastHit == null) {
                data.lastHit = System.currentTimeMillis()
            }
        }
    }

    @EventHandler
    private fun onTridentLaunch(event: PlayerLaunchProjectileEvent) {
        if (event.player !in playerData) return
        if (event.projectile !is Trident) return
        if (!bloodyMacheteCompare(event.itemStack)) return
        val data = playerData[event.player] ?: return
        event.isCancelled = true
        event.player.inventory.remove(event.itemStack)
        val loc = event.player.location.clone()
        val snapLoc = loc.clone()
        loc.yaw = 0.0f
        loc.pitch = 0.0f
        loc.y += 1.0
//        loc.yaw += 90.0f
//        loc.pitch += 90.0f
        var idisp: Entity? = null
        val gep =
            GenericEntityProjectile(
                {
                    val ib = ItemBuilder(Material.STONE) {
                        itemModel(NamespacedKey("horror", "weapons/machete_bloody"))
                    }
                    val idisplay = event.player.world.spawn(
                        loc,
                        ItemDisplay::class.java
                    )
                    idisplay.teleportDuration = 2
                    idisplay.interpolationDelay = 0
                    idisplay.interpolationDuration = 2
                    idisplay.isGlowing = true
                    idisplay.glowColorOverride = Color.RED
                    idisplay.setItemStack(ib.build())
                    idisplay.transformation = ItemDisplayUtils.rotateToPlayerFacing(event.player)
                    idisp = idisplay
                    idisplay
                },
                direction = event.player.location.direction.clone().normalize(),
                collision = SinglePointCollisionCalculator(),
                options = GenericProjectileOptions(
                    velocity = 1.0,
                    physicsStep = 0.1
                ),
                callback = object : ItemDisplayProjectileCallback {
                    private var pitchOffset : Double = 0.0
                    override fun onStep(entity: Entity) {
                        val l = entity.location.clone()
                        pitchOffset = ((pitchOffset + 35.0) % 360.0)
                        if (entity is Display) {
                            entity.transformation = ItemDisplayUtils.rotateToYawPitch(
                                snapLoc.yaw, snapLoc.pitch, pitchOffset
                            )
                        }
                    }

                    override fun onHit(entity: Entity) {
                    }
                }
            )
        ProjectileUtils.launch(
            context.survivorTeam.players.toList(),
            gep,
            { pcr ->
                when (pcr) {
                    is ProjectileCollisionResult.BlockResult -> {
                        data.state.unload()
                        val state = MachetePlayerState.ThrownIntoBlock(
                            event.player, idisp!! as Display, macheteIb.build()
                        )
                        data.state = state
                        PDCUtils.setString(
                            idisp, "machete_owner", event.player.uniqueId.toString()
                        )
                        state.addReplenishTask {
                            data.state.unload()
                            data.state = MachetePlayerState.Idle
                        }
                        false
                    }
                    is ProjectileCollisionResult.EntityResult -> {
                        idisp?.remove()
                        if (pcr.entity is Player) {
                            context.survivorHealth.syntheticDamagePlayer(
                                event.player, pcr.entity as Player, 65f
                            )
                        }
                        if (!context.gameProbablyOver()) event.player.give(macheteIb.build())
                        false
                    }
                    ProjectileCollisionResult.NoneResult -> true
                }
            },
            interval = 1L,
            maxSteps = 20*10,
            callback = object : ProjectileLaunchCallback {
                override fun onMaxSteps() {
                    idisp?.remove()
                    if (!context.gameProbablyOver()) event.player.give(macheteIb.build())
                }
            }
        )
        event.player.playSound(event.player, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.0F, 1.0F)
    }

    @EventHandler
    private fun onMove(event: PlayerMoveEvent) {
        if (event.player !in playerData) return
        val data = playerData[event.player] ?: return
        val state = data.state
        if (state !is MachetePlayerState.ThrownIntoBlock) return
        val distanceSq = safeDistanceSquared(state.display.location, event.player.location)
        if (distanceSq == null || distanceSq > 10) return
        state.unload()
        data.state = MachetePlayerState.Idle
        event.player.give(bloodyMacheteIb.build())
    }

    @EventHandler(priority = EventPriority.HIGH)
    private fun onDamage(event: EntityDamageByEntityEvent) {
        dmgHandler(event)
    }

    private var dmgLock = false
    private fun dmgHandler(event: EntityDamageByEntityEvent) {
        if (dmgLock) return
        if (event.damager !is Player) return
        if (event.entity !is Player) return
        val data = playerData[event.damager] ?: return
        val damager = event.damager as Player
        val victim = event.entity as Player
        if (!data.canHit()) {
            event.isCancelled = true
            return
        }
        val isNormal = normalMacheteCompare(damager.inventory.itemInMainHand)
        val isBloody = bloodyMacheteCompare(damager.inventory.itemInMainHand)
        if (isNormal || isBloody) {
            dmgLock = true
//            HACKS.hurtAnimation(
//            context.playerAccess.all.toList(),
//                victim,
//                damager.yaw
//            )
            context.survivorHealth.overrideOrganicDamage(
                victim, event,  if (isBloody) 35f else 18f
            )
            replaceWithBloody(damager)
            dmgLock = false
            data.lastHit = System.currentTimeMillis()
        }

    }

    override fun onPlayerKill(killer: Player, victim: Player, damageInfo: EntityDamageEvent) {
        replaceWithBloody(killer)
    }

    private fun replaceWithBloody(player: Player) {
        val data = playerData[player] ?: return
        val isNormal = normalMacheteCompare(player.inventory.itemInMainHand)
        if (!isNormal) return
        player.inventory.setItem(EquipmentSlot.HAND, bloodyMacheteIb.build())
    }

    override fun remove(player: Player) {
        playerData -= player
    }

    override fun unload(external: Boolean) {
        dump.destroyAll()
        unregisterListener(this)
        playerData.values.forEach { mps ->
            mps.state.unload()
        }
        playerData.unload(external)
    }
}

class MachetePlayerData(
    var lastHit: Long? = null,
    var state: MachetePlayerState = MachetePlayerState.Idle
) {
    fun canHit() : Boolean {
        if (lastHit == null) return true
        return System.currentTimeMillis() - lastHit!! > 1500
    }
}

sealed class MachetePlayerState : UnloadableResource {
    object Idle : MachetePlayerState() {
        override fun unload(external: Boolean) {}
    }

    class ThrownIntoBlock(
        private val player: Player,
        val display: Display,
        private val machete: ItemStack
    ) : MachetePlayerState() {
        private val dump = GlobalResourceTrackers.createResourceDump()

        fun addReplenishTask(completeFn: () -> Unit) {
            dump.addSelfTracking(
                GlobalResourceTrackers.scheduler.afterUntracked(20L * 10L) {
                    player.give(machete)
                    completeFn()
                }
            )
            dump.add(
                MacheteIndicatorAnimation(
                    player, display.location.clone().add(0.0, 0.2, 0.0)
                )
            )
        }

        override fun unload(external: Boolean) {
            display.remove()
            dump.destroyAll()
        }
    }
}

class MacheteIndicatorAnimation(
    player: Player, location: Location
) : UnloadableResource {
    private val dump = GlobalResourceTrackers.createResourceDump()
    init {
        val me = ManagedEntity.textDisplay(
        location.toVector(),
            ClientEntity.textDisplay(location.toVector()),
            setOf(ManagedEntityOptions.DYNAMIC_SCALING)
        )
        me.apply(player)
        dump.add(me)
        me.metadataUpdate(
        player,
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

    override fun unload(external: Boolean) {
        dump.destroyAll()
    }
}