package com.empire.forest.hud

import com.empire.ignite.display.PlayerNametags
import com.empire.ignite.game.facets.QueueEvent
import com.empire.ignite.game.facets.QueueSubscriber
import com.empire.ignite.util.UnloadableResource
import com.empire.ignite.util.collection.OnlinePlayerData
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class QueueNametagIndicator(plugin: JavaPlugin) : QueueSubscriber {
    private val tagData : OnlinePlayerData<UnloadableResource> = OnlinePlayerData(
        plugin, onEvict = {_, d -> d.unload()}
    )
//    private val fakeTeamName : String = "g_${UUID.randomUUID()}"
//    private val team : Team

    init {
//        val sbTeam = ScoreboardAllocator.registerMainScoreboardTeam(fakeTeamName)
//        sbTeam.color(NamedTextColor.GREEN)
//        ScoreboardAllocator.propagateUpdatedTeamProperties(sbTeam)
//        team = sbTeam
    }

    override fun onQueue(player: Player) {
        val tagTask = PlayerNametags.getTagTask(player) {
            Component.text("Queued").color(NamedTextColor.GREEN)
        }
        tagTask.load()
        tagData[player] = tagTask
//        ScoreboardAllocator.addPlayerToTeam(fakeTeamName, player)
    }

    override fun onDequeue(player: Player) {
        tagData.remove(player)?.unload()
//        ScoreboardAllocator.removePlayerFromTeam(fakeTeamName, player)
    }

    override fun onQueueEvent(event: QueueEvent) {
        if (event == QueueEvent.COMPLETE) {
            tagData.values.forEach { it.unload() }
            tagData.unload()
//            ScoreboardAllocator.unregisterMainScoreboardTeam(team)
        }
    }
}