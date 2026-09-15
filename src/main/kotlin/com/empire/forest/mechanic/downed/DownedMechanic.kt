package com.empire.forest.mechanic.downed

import com.empire.forest.ForestContext
import com.empire.forest.constants.ForestConstants
import com.empire.forest.perk.MedicPerk
import com.empire.forest.util.ForestUtils.fakeGlow
import com.empire.hacks.common.builder.ClientEntityMetadataHelpers
import com.empire.ignite.display.DisplayUpdater
import com.empire.ignite.display.Displays
import com.empire.ignite.game.facets.DeathEvents
import com.empire.ignite.game.facets.system.PlayerRemoveReason
import com.empire.ignite.util.GlobalResourceTrackers
import com.empire.ignite.util.HACKS
import com.empire.ignite.util.UnloadableResource
import com.empire.ignite.util.collection.OnlinePlayerData
import com.empire.ignite.util.entity.ClientEntity
import com.empire.ignite.util.entity.EntityModelBuilder
import com.empire.ignite.util.entity.ManagedEntity
import com.empire.ignite.util.entity.ManagedEntityOptions
import com.empire.ignite.util.healAndClearInventoryOfPlayers
import com.empire.ignite.util.registerListener
import com.empire.ignite.util.scoreboard.ScoreboardAllocator
import com.empire.ignite.util.text.TextUtils.flattenComponents
import com.empire.ignite.util.unregisterListener
import io.papermc.paper.datacomponent.item.ResolvableProfile
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Display
import org.bukkit.entity.Entity
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Player
import org.bukkit.entity.Pose
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.plugin.java.JavaPlugin
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

class DownedMechanic(
    plugin: JavaPlugin, private val context: ForestContext
) : UnloadableResource, DeathEvents, Listener {
    private val downedData : OnlinePlayerData<DownedEntityData> = OnlinePlayerData(
        plugin,
        onEvict = {_, r -> r.unload()}
    )
    private val reviverData : OnlinePlayerData<ReviverData> = OnlinePlayerData(
        plugin,
        onEvict = {_, r -> r.unload()}
    )
    private val dump = GlobalResourceTrackers.createResourceDump()

    companion object {
        val TARGET_DEFIB_QUANTITY = 150
        val BODY_LINGER_TIME = 75.toDuration(DurationUnit.SECONDS)
    }

    init {
        registerListener(plugin, this)
        dump.add(GlobalResourceTrackers.scheduler.repeatTask(4L, ::tick))
        createFakeOrangeTeam()
    }

    private fun tick() {
        val medicPerk = context.getPerk(MedicPerk::class.java) ?: return
        if (medicPerk !is MedicPerk) return
        val activeRevivers : MutableSet<Player> = mutableSetOf()
        val now = Clock.System.now()
        downedData.entries.toList().forEach { (_, value) ->
            val downed = value.downed
            if (value.player !in context.playerAccess.spectating) {
                downedData.remove(value.player)?.unload()
                return@forEach
            }
            if (now - value.spawned >= BODY_LINGER_TIME) {
                downedData.remove(value.player)?.unload()
                return@forEach
            }
            context.playerAccess.all.forEach { survivor ->
//            context.survivorTeam.players.forEach { survivor ->
                if (!medicPerk.using(survivor)) return@forEach
                if (survivor.location.distanceSquared(downed.location) > 5*5) return@forEach
                if (!HACKS.lookingAtEntity(survivor, downed, maxDistance = 7.0, raySize = 1.25))
                    return@forEach
                fakeGlow<Entity>(
                    listOf(survivor), listOf(downed), 5L
                )
                getReviveData(survivor) ?: createReviveData(survivor, value)
                activeRevivers += survivor
            }
        }
        (reviverData.keys - activeRevivers).forEach { k ->
            reviverData.remove(k)?.unload()
        }
    }

    private fun getReviveData(player: Player) : ReviverData? =
        reviverData[player]

    private fun createReviveData(
        player: Player,
        downedEntityData: DownedEntityData
    ) : ReviverData {
        val rd = ReviverData(player, context, target = downedEntityData)
        reviverData[player] = rd
        return rd
    }

    @EventHandler
    private fun onInteract(event: PlayerInteractAtEntityEvent) {
        val player = event.player
        val reviveData = reviverData[player] ?: return
        reviveData.target.defibProgress = min(TARGET_DEFIB_QUANTITY, reviveData.target.defibProgress + 1)
        if (reviveData.target.defibProgress >= TARGET_DEFIB_QUANTITY) {
            reviveData.target.unload()
            reviveData.unload()
            if (reviveData.target.player in context.playerAccess.spectating) {
                context.playerTracker.removeSpectator(reviveData.target.player, PlayerRemoveReason.SYSTEM)
                context.playerTracker.addPlayer(reviveData.target.player)
                context.addSurvivor(reviveData.target.player)
                healAndClearInventoryOfPlayers(listOf(reviveData.target.player))
                reviveData.target.player.gameMode = GameMode.ADVENTURE
                reviveData.target.player.teleport(reviveData.player)
            }
        }
    }

    override fun onPlayerDeath(player: Player, damageInfo: EntityDamageEvent) {
        if (player in context.hunterTeam.players) return
        spawnDownedEntity(player)
    }

    override fun unload(external: Boolean) {
        downedData.values.forEach { it.unload(external) }
        downedData.unload(external)
        reviverData.values.forEach { it.unload(external) }
        reviverData.unload(external)
        dump.destroyAll()
        unregisterListener(this)
    }

    private fun spawnDownedEntity(player: Player) {
        if (player in downedData.keys) return
        val emb = EntityModelBuilder(Mannequin::class.java) {
            onSpawn {
                this.profile = ResolvableProfile.resolvableProfile(
                    Bukkit.getServer().createProfile(player.uniqueId)
                )
                this.pose = Pose.SWIMMING
                this.isInvulnerable = true
                this.isCollidable = false
            }
        }
        val entity = emb.spawn(player.location.clone().add(0.0, 0.3, 0.0))
        ScoreboardAllocator.addEntityToTeam("downed_orangish", entity.uniqueId)
        downedData += player to DownedEntityData(player, context, entity, Clock.System.now())
    }

    private fun createFakeOrangeTeam() {
        val sbTeam = ScoreboardAllocator.registerMainScoreboardTeam("downed_orangish")
        sbTeam.color(NamedTextColor.GOLD)
        ScoreboardAllocator.propagateUpdatedTeamProperties(sbTeam)
    }
}

class DownedEntityData(
    val player: Player,
    val context: ForestContext,
    val downed: Mannequin,
    val spawned: Instant,
    var defibProgress: Int = 0,
) : UnloadableResource {
    val unloadables: MutableList<UnloadableResource> = mutableListOf()
    val infoDisplay : ManagedEntity

    init {
        unloadables += GlobalResourceTrackers.scheduler.repeatTask(5L) {
            defibProgress = max(0, defibProgress - 1)
        }

        val tagSpawnLoc = downed.eyeLocation.clone().add(0.0, 0.5, 0.0).toVector()
        infoDisplay = ManagedEntity.textDisplay(
            tagSpawnLoc,
            ClientEntity.textDisplay(tagSpawnLoc),
            options = setOf(ManagedEntityOptions.DYNAMIC_SCALING)
        )

        val medicPerk = context.getPerk(MedicPerk::class.java)
        if (medicPerk != null && medicPerk is MedicPerk) {
            unloadables += bindAtInterval(
                { context.playerAccess.alive.filter { p -> medicPerk.using(p) }.toSet() },
                5L,
                { t ->
                        infoDisplay.apply(t)
                        infoDisplay.metadataUpdate(
                            t,
                            listOf(
                                ClientEntityMetadataHelpers.textDisplayText(
                                    flattenComponents(
                                        Component.text("DEAD").color(NamedTextColor.RED),
                                        Component.text(" | ").color(NamedTextColor.DARK_GRAY),
                                        Component.text(player.name).color(ForestConstants.SURVIVORS_COLOR)
                                    )
                                ),
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
                },
                { t ->
                    infoDisplay.destroy(t)
                }
            )
        }
    }

    override fun unload(external: Boolean) {
        downed.remove()
        unloadables.forEach { u -> u.unload(external) }
        unloadables.clear()
        infoDisplay.unload(external)
    }
}

private fun bindAtInterval(
    targets: () -> Set<Player>,
    interval: Long,
    apply: (Player) -> Unit,
    remove: (Player) -> Unit
) : UnloadableResource {
    val lastApplied = mutableSetOf<Player>()
    return GlobalResourceTrackers.scheduler.repeatTask(interval) {
        val current = targets()
        val gone = lastApplied - current
        val newPlayers = current - lastApplied
        gone.forEach { remove(it) }
        newPlayers.forEach { apply(it) }
    }
}

class ReviverData(
    val player: Player,
    val context: ForestContext,
    val target: DownedEntityData
) : UnloadableResource {
    companion object {
        private val UPDATE_INTERVAL = 3
    }

    val unloadables : MutableList<UnloadableResource> = mutableListOf()

    init {
        val taskId = Displays.getSubtitle(player).displayQueueManager.submitTask(
            DisplayUpdater.createTitleUpdater(
                player,
                ::cookem,
                updateInterval = UPDATE_INTERVAL.toLong()
            )
        )
        val tagSpawnLoc = target.downed.eyeLocation.clone().add(0.0, 0.5, 0.0).toVector()
        unloadables += object : UnloadableResource {
            override fun unload(external: Boolean) {
                Displays.getSubtitle(player).displayQueueManager.removeByID(taskId)
            }
        }
    }

    private fun cookem() : Title {
        val now = Clock.System.now()
        val duration = now.minus(target.spawned)
        val total = DownedMechanic.BODY_LINGER_TIME
        var remain = total.minus(duration)
        if (remain.isNegative()) {
            remain = Duration.ZERO
        }
        val minutes = remain.inWholeMinutes
        val seconds = remain.inWholeSeconds % 60
        val timePart = Component.text("${(if (minutes == 0L) "" else "${minutes}m")}${seconds}s")
            .color(NamedTextColor.YELLOW)
        val totalBarCount = 20
        val bars = min(totalBarCount, target.defibProgress / (DownedMechanic.TARGET_DEFIB_QUANTITY / totalBarCount))
        val progressPart = flattenComponents(
            Component.text("|".repeat(bars))
                .color(NamedTextColor.GREEN),
            Component.text("|".repeat(totalBarCount - bars))
                .color(NamedTextColor.RED),
        )
        return Title.title(
            Component.empty(),
            flattenComponents(
                Component.text("SPAM RIGHT CLICK!").color(NamedTextColor.BLUE),
                Component.text(" | ").color(NamedTextColor.DARK_GRAY),
                timePart,
                Component.text(" "),
                progressPart
            ),
            0,  UPDATE_INTERVAL + 2, 0
        )
    }

    override fun unload(external: Boolean) {
        unloadables.forEach { it.unload() }
        unloadables.clear()
    }
}