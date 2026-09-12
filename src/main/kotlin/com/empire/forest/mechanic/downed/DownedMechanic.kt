package com.empire.forest.mechanic.downed

import com.empire.forest.ForestContext
import com.empire.forest.util.ForestUtils.fakeGlow
import com.empire.ignite.game.facets.DeathEvents
import com.empire.ignite.util.GlobalResourceTrackers
import com.empire.ignite.util.HACKS
import com.empire.ignite.util.UnloadableResource
import com.empire.ignite.util.collection.OnlinePlayerData
import com.empire.ignite.util.entity.EntityModelBuilder
import com.empire.ignite.util.registerListener
import com.empire.ignite.util.scoreboard.ScoreboardAllocator
import com.empire.ignite.util.unregisterListener
import io.papermc.paper.datacomponent.item.ResolvableProfile
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Player
import org.bukkit.entity.Pose
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.java.JavaPlugin
import kotlin.time.Clock
import kotlin.time.Instant

class DownedMechanic(
    plugin: JavaPlugin, private val context: ForestContext
) : UnloadableResource, DeathEvents, Listener {
    private val data : OnlinePlayerData<DownedEntityData> = OnlinePlayerData(plugin)
    private val dump = GlobalResourceTrackers.createResourceDump()

    init {
        registerListener(plugin, this)
        dump.add(GlobalResourceTrackers.scheduler.repeatTask(4L, ::tick))
        createFakeOrangeTeam()
    }

    private fun tick() {
        data.entries.toList().forEach { (_, value) ->
            val downed = value.downed
            if (value.player !in context.playerAccess.spectating) {
                data.remove(value.player)?.unload()
                return@forEach
            }
            context.playerAccess.alive.forEach { survivor ->
                if (survivor.location.distanceSquared(downed.location) > 5*5) return@forEach
                if (!HACKS.lookingAtEntity(survivor, downed, maxDistance = 7.0, raySize = 1.25))
                    return@forEach
                fakeGlow<Entity>(
                    listOf(survivor), listOf(downed), 5L
                )
            }
        }
    }

    @EventHandler
    private fun onInteract(event: PlayerInteractAtEntityEvent) {

    }

    override fun onPlayerDeath(player: Player, damageInfo: EntityDamageEvent) {
        if (player in context.hunterTeam.players) return
        spawnDownedEntity(player)
    }

    override fun unload(external: Boolean) {
        data.values.forEach { it.unload(external) }
        data.unload(external)
        dump.destroyAll()
        unregisterListener(this)
    }

    private fun spawnDownedEntity(player: Player) {
        if (player in data.keys) return
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
        data += player to DownedEntityData(player, entity, Clock.System.now())
    }

    private fun createFakeOrangeTeam() {
        val sbTeam = ScoreboardAllocator.registerMainScoreboardTeam("downed_orangish")
        sbTeam.color(NamedTextColor.GOLD)
        ScoreboardAllocator.propagateUpdatedTeamProperties(sbTeam)
    }
}

class DownedEntityData(
    val player: Player,
    val downed: Mannequin,
    val spawned: Instant
) : UnloadableResource {
    override fun unload(external: Boolean) {
        downed.remove()
    }
}