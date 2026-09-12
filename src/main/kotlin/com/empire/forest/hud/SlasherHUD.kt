package com.empire.forest.hud

import com.empire.forest.ForestContext
import com.empire.forest.kit.ForestKitKey
import com.empire.forest.kit.HunterKit
import com.empire.forest.perk.BloodlustPerk
import com.empire.forest.perk.FearPerk
import com.empire.forest.perk.ShadowPerk
import com.empire.ignite.display.Displays
import com.empire.ignite.game.kit.KitTracker
import com.empire.ignite.util.ChangingSetReceiver
import com.empire.ignite.util.IgniteResource
import com.empire.ignite.util.collection.OnlinePlayerData
import com.empire.ignite.util.text.TextUtils.flattenComponents
import com.empire.ignite.util.withSeperatingElement
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class SlasherHUD(
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

    private fun isUsingSlasherClass(player: Player) : Boolean {
        if (player !in context.hunterTeam.players) return false
        val kit = kitTracker.getAppliedKitForPlayer(player) ?: return false
        return kit.gameKit == HunterKit.SLASHER.gameKit
    }

    private fun setupActionBar(player: Player) {
        if (!isUsingSlasherClass(player)) return
        if (hudData[player] != -1L && hudData[player] != null) return
        hudData[player] = Displays.getActionBar(player).displayQueueManager.submitTask {
            val fearPerk = context.getPerk(FearPerk::class.java)
            if (fearPerk == null || fearPerk !is FearPerk) return@submitTask Component.empty()
            val fearPerkChar = if (fearPerk.cooldownAbility.cooldown.canUse(player)) "\uE115" else "\uE114"
            val fearComponent = Component.text(fearPerkChar)
            val fearTime = formatTime(fearPerk.cooldownAbility.cooldown.getRemainingCooldownTicks(player))

            val bloodlustPerk = context.getPerk(BloodlustPerk::class.java)
            if (bloodlustPerk == null || bloodlustPerk !is BloodlustPerk) return@submitTask Component.empty()
            val bloodlustPerkChar = if (bloodlustPerk.cooldownAbility.cooldown.canUse(player)) "\uE117" else "\uE116"
            val bloodlustComponent = Component.text(bloodlustPerkChar)
            val bloodTime = formatTime(bloodlustPerk.cooldownAbility.cooldown.getRemainingCooldownTicks(player))

            val shadowsPerk = context.getPerk(ShadowPerk::class.java)
            if (shadowsPerk == null || shadowsPerk !is ShadowPerk) return@submitTask Component.empty()
            val shadowsPerkChar = if (shadowsPerk.cooldown.cooldown.canUse(player)) "\uE119" else "\uE118"
            val shadowsComponent = Component.text(shadowsPerkChar)
            val shadowTime = formatTime(shadowsPerk.cooldown.cooldown.getRemainingCooldownTicks(player))

            flattenComponents(
                *(
                        listOf(
                            listOf(fearComponent, fearTime)
                                .withSeperatingElement(Component.text(" ")),
                            listOf(bloodlustComponent, bloodTime)
                                .withSeperatingElement(Component.text(" ")),
                            listOf(shadowsComponent, shadowTime)
                                .withSeperatingElement(Component.text(" ")),
                        ).withSeperatingElement(listOf(Component.text(" ".repeat(3)))).flatten()
                        ).toTypedArray()
            )
        }
    }

    private fun formatTime(ticks: Long) : Component {
        val timeComponent = Component.text(
            if (ticks == 0L) "Ready" else
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