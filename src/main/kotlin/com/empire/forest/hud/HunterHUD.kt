package com.empire.forest.hud

import com.empire.forest.ForestContext
import com.empire.forest.kit.ForestKitKey
import com.empire.forest.kit.HunterKit
import com.empire.forest.mechanic.BearTrap
import com.empire.forest.perk.BearTrapPerk
import com.empire.forest.perk.ForestPerk
import com.empire.forest.perk.NewMotionSensorPerk
import com.empire.ignite.display.Displays
import com.empire.ignite.game.kit.KitTracker
import com.empire.ignite.util.ChangingSetReceiver
import com.empire.ignite.util.IgniteResource
import com.empire.ignite.util.collection.OnlinePlayerData
import com.empire.ignite.util.text.TextUtils.flattenComponents
import com.empire.ignite.util.withSeperatingElement
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import kotlin.collections.flatten
import kotlin.math.max
import kotlin.math.min

class HunterHUD(
    plugin: JavaPlugin,
    private val context: ForestContext,
    private val kitTracker: KitTracker<ForestKitKey, ForestContext>
) : IgniteResource {
    private val hudData : OnlinePlayerData<Long> = OnlinePlayerData(plugin, onEvict = { p, id ->
        Displays.getActionBar(p).displayQueueManager.removeByID(id)
    })

    override fun load() {
        context.hunterTeamChangeSetContract.changeSet.setupOnAdd(object : ChangingSetReceiver<Player> {
            override fun onAdd(item: Player) {
                setupActionBar(item)
            }
        })
        context.hunterTeamChangeSetContract.changeSet.setupOnRemove(object : ChangingSetReceiver<Player> {
            override fun onRemove(item: Player) {
                disableActionBar(item)
            }
        })
        context.hunterTeam.players.forEach {
            setupActionBar(it)
        }
    }

    fun onKitSelection() {
        context.hunterTeam.players.forEach {
            setupActionBar(it)
        }
    }

    private fun isUsingHunterClass(player: Player) : Boolean {
        if (player !in context.hunterTeam.players) return false
        val kit = kitTracker.getAppliedKitForPlayer(player) ?: return false
        return kit.gameKit == HunterKit.SLASHER.gameKit
    }

    private fun setupActionBar(player: Player) {
        if (!isUsingHunterClass(player)) return
        if (hudData[player] != -1L && hudData[player] != null) return
        hudData[player] = Displays.getActionBar(player).displayQueueManager.submitTask {
            val data = context.getPerk(BearTrapPerk::class.java)?.contextManager?.getData(player)
                ?: return@submitTask Component.empty()
            val unicodeChar = min(max(0, data.lru.buffer.size), 8)
            var base = 0xE100
            base += unicodeChar
            val trapComponent = Component.text(String(Character.toChars(base)))

            val remainingTime = data.activeTimer?.getRemainingTicks() ?: 0L
            val timeComponent = Component.text(
                "%.2f".format((remainingTime * 50) / 1000.0)
            ).color(
                when (remainingTime) {
                    0L -> NamedTextColor.GREEN
                    in 0L..(3*20L) -> NamedTextColor.YELLOW
                    else -> NamedTextColor.RED
                }
            )

            val sensorPerk : ForestPerk<*>? = context.getPerk(NewMotionSensorPerk::class.java)
            if (sensorPerk == null || sensorPerk !is NewMotionSensorPerk) return@submitTask Component.empty()
            if (!sensorPerk.using(player)) return@submitTask Component.empty()
            var sensorBaseChar = 0xE110
            if (sensorPerk.playerData[player]?.buf?.buffer?.size == 1) {
                sensorBaseChar = 0xE111
            }
            val sensorCharComponent =
                Component.text(String(Character.toChars(sensorBaseChar)))
            val sensorTicksComponent = formatTime(sensorPerk.cooldown.getRemainingCooldownTicks(player))

            flattenComponents(
                *(
                    listOf(
                        listOf(trapComponent, timeComponent)
                            .withSeperatingElement(Component.text(" ")),
                        listOf(sensorCharComponent, sensorTicksComponent)
                            .withSeperatingElement(Component.text(" "))
                    ).withSeperatingElement(listOf(Component.text(" ".repeat(3)))).flatten()
                ).toTypedArray()
            )
        }
    }

    private fun formatTime(ticks: Long) : Component {
        val timeComponent = Component.text(
            "%.2f".format((ticks * 50) / 1000.0)
        ).color(
            when (ticks) {
                0L -> NamedTextColor.GREEN
                in 0L..(3*20L) -> NamedTextColor.YELLOW
                else -> NamedTextColor.RED
            }
        )
        return timeComponent
    }

    private fun disableActionBar(player: Player) {
        val id = hudData[player]
        if (id == null || id == -1L) return
        Displays.getActionBar(player).displayQueueManager.removeByID(id)
    }

    override fun unload(external: Boolean) {
        hudData.keys.forEach { disableActionBar(it)  }
        hudData.unload(external)
    }
}