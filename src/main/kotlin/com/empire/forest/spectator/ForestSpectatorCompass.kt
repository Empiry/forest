package com.empire.forest.spectator

import com.empire.forest.ForestContext
import com.empire.forest.resourcepack.ResourcePackConstants
import com.empire.forest.util.ForestUtils.fakeGlowPersistent
import com.empire.ignite.display.DisplayUpdater
import com.empire.ignite.display.Displays
import com.empire.ignite.game.facets.DeathEvents
import com.empire.ignite.util.GlobalResourceTrackers
import com.empire.ignite.util.ItemActiveListenerOperations
import com.empire.ignite.util.UnloadableResource
import com.empire.ignite.util.collection.OnlinePlayerData
import com.empire.ignite.util.denyItemDrop
import com.empire.ignite.util.item.ItemBuilder
import com.empire.ignite.util.itemBuilderActiveListenerPokeable
import com.empire.ignite.util.itemBuilderClickListener
import com.empire.ignite.util.text.TextUtils.flattenComponents
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.plugin.java.JavaPlugin

class ForestSpectatorCompass(
    plugin: JavaPlugin,
    private val context: ForestContext
) : UnloadableResource, DeathEvents {
    val itemBuilder = ItemBuilder(Material.RECOVERY_COMPASS) {
        name(Component.text("Player Tracker").color(NamedTextColor.AQUA))
        unbreakable()
    }
    private val data : OnlinePlayerData<ForestCompassPlayerData> = OnlinePlayerData(
        plugin,
        onEvict = {_, d -> d.unload() }
    )
    private val dump = GlobalResourceTrackers.createResourceDump()
    private val accessor = ForestPlayerAccessor(context)
    private val pokeableItemCheck : ItemActiveListenerOperations

    init {
        dump.add(denyItemDrop(plugin, itemBuilder))
        pokeableItemCheck = itemBuilderActiveListenerPokeable(plugin, itemBuilder, ::onActive)
        dump.add(pokeableItemCheck)
        dump.add(itemBuilderClickListener(plugin, itemBuilder, ::onUse))
        dump.add(GlobalResourceTrackers.scheduler.repeatTask(60L) {
            data.keys.forEach { pokeableItemCheck.poke(it) }
        })
        context.messageBus.add(this)
    }

    fun apply(player: Player) {
        data[player] = ForestCompassPlayerData(player)
        pokeableItemCheck.poke(player)
    }

    fun remove(player: Player) {
        data.remove(player)?.unload()
    }

    private fun onActive(player: Player, active: Boolean) {
        if (active) {
            calibrate(player)
            setupActionBar(player)
        } else {
            removeExistingGlow(player)
            disableActionBar(player)
        }
    }

    private fun onUse(player: Player, action: Action) {
        val data = getData(player) ?: return
        if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
            removeExistingGlow(player)
            data.playerSelIndex++
            if (data.playerSelIndex >= accessor.getSize()) {
                data.playerSelIndex = 0
            }
            calibrate(player)
        } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            accessor.getPlayer(data.playerSelIndex)?.let { target -> player.teleport(target) }
        }
    }

    private fun setupActionBar(player: Player) {
        val data = getData(player) ?: return
        if (data.actionBarTask != -1L) return
        data.actionBarTask = Displays.getActionBar(player).displayQueueManager.submitTask(
            object : DisplayUpdater<Component> {
                private var debounceTicks : Long = 0
                private var cachedDistance : Double? = null
                override fun getUpdate(): Component {
                    val target = accessor.getPlayer(data.playerSelIndex) ?: return Component.empty()
                    debounceTicks++
                    if (debounceTicks % 3L == 0L || cachedDistance == null) {
                        if (target.location.world == player.location.world) {
                            cachedDistance = target.location.distance(player.location)
                        }
                    }
                    return flattenComponents(
                        context.getDisplayName(target),
                        Component.text(" | ").color(NamedTextColor.DARK_GRAY),
                        Component.text("%.2f".format(cachedDistance ?: 0.0))
                            .color(NamedTextColor.YELLOW),
                        Component.text("m").color(NamedTextColor.GRAY),
                        Component.text(" | ").color(NamedTextColor.DARK_GRAY),
                        Component.text(ResourcePackConstants.LMB_ICON),
                        Component.text(" Teleport")
                            .color(NamedTextColor.RED),
                        Component.text(" | ").color(NamedTextColor.DARK_GRAY),
                        Component.text(ResourcePackConstants.RMB_ICON),
                        Component.text(" Cycle")
                            .color(NamedTextColor.BLUE)
                    )
                }
            }
        )
    }

    private fun disableActionBar(player: Player) {
        val data = getData(player) ?: return
        data.cancelActionBarTask()
        player.sendActionBar(Component.empty())
    }

    private fun calibrate(player: Player) {
        removeExistingGlow(player)
        val data = getData(player) ?: return
        val target = accessor.getPlayer(data.playerSelIndex) ?: return
        data.glowUnloadable = fakeGlowPersistent(
            listOf(player),
            listOf(target)
        )
    }

    private fun removeExistingGlow(player: Player) {
        val data = getData(player) ?: return
        data.glowUnloadable?.unload()
        data.glowUnloadable = null
    }

    private fun getData(player: Player) : ForestCompassPlayerData? {
        return data[player]
    }

    override fun unload(external: Boolean) {
        dump.destroyAll()
        data.values.forEach { it.unload(external) }
        data.unload(external)
    }
}

private data class ForestCompassPlayerData(
    val player: Player,
    var playerSelIndex: Int = 0,
    var glowUnloadable: UnloadableResource? = null,
    var actionBarTask : Long = -1L
) : UnloadableResource {
    override fun unload(external: Boolean) {
        glowUnloadable?.unload(external)
        glowUnloadable = null
        cancelActionBarTask()
    }

    fun cancelActionBarTask() {
        if (actionBarTask != -1L) {
            Displays.getActionBar(player).displayQueueManager.removeByID(actionBarTask)
            actionBarTask = -1L
        }
    }
}

private class ForestPlayerAccessor(
    private val context: ForestContext
) {
    fun getPlayer(index: Int) : Player? {
        val view = (context.hunterTeam.players + context.survivorTeam.players).toList()
        return if (index < 0 || index >= view.size) null
        else view.get(index)
    }

    fun getSize() : Int =
        (context.hunterTeam.players + context.survivorTeam.players).size
}