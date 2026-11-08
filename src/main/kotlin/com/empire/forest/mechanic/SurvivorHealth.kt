package com.empire.forest.mechanic

import com.empire.forest.ForestContext
import com.empire.forest.perk.ComponentShape
import com.empire.ignite.display.Displays
import com.empire.ignite.game.facets.DeathEvents
import com.empire.ignite.util.ChangingSetReceiver
import com.empire.ignite.util.HACKS
import com.empire.ignite.util.IgniteResource
import com.empire.ignite.util.UnloadableResource
import com.empire.ignite.util.collection.OnlinePlayerData
import com.empire.ignite.util.registerListener
import com.empire.ignite.util.text.TextUtils.flattenComponents
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffectType
import kotlin.math.max

class SurvivorHealth(
    private val plugin: JavaPlugin,
    private val context: ForestContext
) : IgniteResource, Listener {
    private val targets : OnlinePlayerData<HealthPlayerData> = OnlinePlayerData(plugin)

    override fun load() {
        registerListener(plugin, this)
        context.survivorTeamChangeSetContract.changeSet.setupOnAdd(object : ChangingSetReceiver<Player> {
            override fun onAdd(item: Player) {
                apply(item)
            }
        })
        context.survivorTeamChangeSetContract.changeSet.setupOnRemove(object : ChangingSetReceiver<Player> {
            override fun onRemove(item: Player) {
                remove(item)
            }
        })
        context.survivorTeam.players.forEach {
            apply(it)
        }
    }

    fun getHealth(player: Player) : HealthPlayerData? = targets[player]

    private fun apply(player: Player) {
        if (player in targets) return
        val healthData = HealthPlayerData(player)
        targets += player to healthData
        healthData.actionBarTaskId = Displays.getActionBar(player).displayQueueManager.submitTask {
            val data = targets[player] ?: return@submitTask Component.empty()
            flattenComponents(
                Component.text("\uE003"),
                context.jamIfNeeded(
                    flattenComponents(
                        Component.text(" ${data.health}"),
                        Component.text("/₁₀₀").color(NamedTextColor.DARK_GRAY)
                    ),
                    ComponentShape(8, NamedTextColor.RED)
                )
            )
        }
    }


    private fun remove(player: Player) {
        if (player !in targets) return
        targets.remove(player)?.unload()
    }

    override fun unload(external: Boolean) {
        targets.values.forEach { hpd -> hpd.unload(external) }
        targets.unload()
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
//    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    private fun onDamage(event: EntityDamageEvent) {
//        println(
//            "DAMAGE EVENT: ${event.entity.name} " +
//                    "cancelled=${event.isCancelled} " +
//                    "damage=${event.damage} " +
//                    "type=${event::class.simpleName}"
//        )
        if (event.entity !is Player) return
        if (event.entity !in targets) return
        if (event is EntityDamageByEntityEvent) {
//            println(
//                "COMBAT: ${event.damager} -> ${event.entity}, " +
//                        "attackerTeam=${context.findPlayerTeam(event.damager as Player)}, " +
//                        "victimTeam=${context.findPlayerTeam(event.entity as Player)}"
//            )
            if (event.damager is Player && event.entity is Player) {
                if (context.findPlayerTeam(event.damager as Player) ==
                    context.findPlayerTeam(event.entity as Player)) {
                    event.isCancelled = true
                    return
                }
            }

        }
        val player : Player = event.entity as Player
        player.health = 20.0
        val data = targets[player] ?: return
        val actualDmg = if (data.dmgOverride != null) data.dmgOverride!! else event.finalDamage.toFloat()
        data.health = max(0, (data.health - actualDmg).toInt())
        event.damage = 1.0
        if (data.health == 0) {
            event.damage = 30.0
            context.messageBus.acquire<DeathEvents> { it.onPlayerDeath(player, event)  }
        }
        data.dmgOverride = null
    }

    fun syntheticDamagePlayer(source: Player, destination: Player, amount: Float) {
        val dstData = targets[destination] ?: return
        dstData.dmgOverride = amount
        HACKS.organicPlayerDamage(source, destination, 0.5f)
        context.stamina?.rewardStamina(destination, amount.toDouble())
    }

    fun syntheticDamage(destination: Player, amount: Float) {
        val dstData = targets[destination] ?: return
        dstData.dmgOverride = amount
        HACKS.organicGenericDamage(destination, 0.5f)
        context.stamina?.rewardStamina(destination, amount.toDouble())
    }

    fun overrideOrganicDamage(destination: Player, ev: EntityDamageEvent, amount: Float) {
        val dstData = targets[destination] ?: return
        dstData.dmgOverride = amount
        ev.damage = 0.5
    }
}

class HealthPlayerData (
    var player: Player,
    var health: Int = 100,
    var actionBarTaskId: Long = -1,
    var dmgOverride: Float? = null
) : UnloadableResource {
    override fun unload(external: Boolean) {
        if (actionBarTaskId != -1L) {
            Displays.getActionBar(player).displayQueueManager.removeByID(actionBarTaskId)
            actionBarTaskId = -1L
        }
    }
}