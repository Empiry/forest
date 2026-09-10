package com.empire.forest.perk

import com.empire.forest.ForestContext
import com.empire.forest.util.ForestUtils.fakeGlow
import com.empire.forest.util.PlayerDataAccess
import com.empire.ignite.util.GlobalResourceTrackers
import com.empire.ignite.util.HACKS
import com.empire.ignite.util.collection.OnlinePlayerData
import com.empire.ignite.util.registerListener
import com.empire.ignite.util.unregisterListener
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.plugin.java.JavaPlugin

class StaredownPerk(
    plugin: JavaPlugin,
    private val context: ForestContext
) : ForestPerk<Unit>, Listener {
    override val contextManager: PlayerDataAccess<Unit> = PlayerDataAccess.noop()
    private val applied : OnlinePlayerData<StaredownPlayerData> = OnlinePlayerData(plugin)
    private val dump = GlobalResourceTrackers.createResourceDump()

    init {
        registerListener(plugin, this)
        dump.add(GlobalResourceTrackers.scheduler.repeatTask(15L, this::recalculate))
    }

    override fun apply(player: Player) {
        applied += (player to StaredownPlayerData())
        player.getAttribute(Attribute.MOVEMENT_SPEED)?.let {
            it.baseValue = context.getNormalMovementSpeed(player)
        }
    }

    fun using(player: Player) =
        player in applied.keys

    fun isSeen(player: Player) : Boolean {
        recalculate()
        val data = applied[player] ?: return false
        return data.isSeen
    }

    override fun remove(player: Player) {
        applied -= player
    }

    @EventHandler(ignoreCancelled = true)
    private fun onMove(event: PlayerMoveEvent) {
        if (event.player !in context.survivorTeam.players) return
        recalculate()
    }

    private fun recalculate() {
        val seenMap = applied.entries.associate { (hunter, data) ->
            hunter to (
                context.survivorTeam.players.filter { survivor ->
                    val dist = survivor.location.distanceSquared(hunter.location)
                    if (dist > 50*50) return@filter false
                    HACKS.lookingAt(survivor, hunter, 50.0, raySize = 1.75)
                } to data
            )
        }
        seenMap.entries.forEach { (hunter, v) ->
            val seeingSurvivors = v.first
            val data = v.second
            if (data.isSeen && seeingSurvivors.isEmpty()) {
                data.isSeen = false
                hunter.foodLevel = 20
            } else if (!data.isSeen && seeingSurvivors.isNotEmpty() && canImpair(hunter)) {
                data.isSeen = true
                hunter.foodLevel = 3
                fakeGlow<Player>(
                    seeingSurvivors, listOf(hunter), 25L
                )
            } else if (!canImpair(hunter) && hunter.foodLevel < 20) {
                hunter.foodLevel = 20
            }
        }
    }

    private fun canImpair(hunter: Player) : Boolean {
        val shadowPerk = context.getPerk(ShadowPerk::class.java) ?: return true
        return !(shadowPerk as ShadowPerk).cloaked(hunter)
    }

    override fun unload(external: Boolean) {
        applied.unload(external)
        unregisterListener(this)
        dump.destroyAll()
    }
}

class StaredownPlayerData(
    var isSeen : Boolean = false
)