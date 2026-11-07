package com.empire.forest.mode

import com.empire.forest.ForestContext
import com.empire.forest.bossbar.SurvivalBossBar
import com.empire.forest.generator.GeneratorDescription
import com.empire.ignite.Ignite
import com.empire.ignite.util.ChangingSetUtils
import com.empire.ignite.util.GlobalResourceTrackers
import com.empire.ignite.util.IgniteResource
import com.empire.ignite.util.timer.Timer
import com.empire.ignite.util.timer.TimerCallback

class ForestSurvivalMode(
    private val plugin: Ignite,
    private val context: ForestContext
) : IgniteResource {
    private val dump = GlobalResourceTrackers.createResourceDump()

    private val BASE_GAME_TICKS = 20L * 60L * 7L
    private var gameTicks = BASE_GAME_TICKS
    private var gameTimer: Timer? = null

    override fun load() {
        gameTicks = BASE_GAME_TICKS + ((context.survivorTeam.players.size - 1) * (40L * 20L))
//        if (context.staticData.debugLever) gameTicks = 20L * 5L
        val bar = SurvivalBossBar(
            plugin, context, gameTicks,
            ChangingSetUtils.unionContractsToView(
                context.getHunterChangeSet(),
                context.getSurvivorChangeSet()
            )
        )
        bar.load()
        val timer = Timer(plugin, gameTicks)
            .attach(object : TimerCallback {
                override fun onTick(originalTicks: Long, remainingTicks: Long) {
                    if (remainingTicks % 20L == 0L) {
                        bar.setTimeRemaining(remainingTicks)
                    }
                    if (remainingTicks % 30L == 0L) {
                        daytimeSync(remainingTicks)
                    }
                }
            })
        timer.start()

        dump.add(timer)
        dump.add(bar)
        gameTimer = timer
    }

    fun onGeneratorComplete(desc: GeneratorDescription) {
        gameTimer?.advance(20L * 30L)
    }

    fun onEnd(callback: () -> Unit) {
        gameTimer?.attach(object : TimerCallback {
            override fun onEnd() {
                callback()
            }
        })
    }

    private val NIGHT_TICKS = 14000L
    private val SUNRISE_TICKS = 23000L

    private fun daytimeSync(ticksRemaining: Long) {
        val timeCalc = (NIGHT_TICKS + (((gameTicks - ticksRemaining) / gameTicks.toDouble()) * (SUNRISE_TICKS - NIGHT_TICKS))).toLong()
        context.world?.time = timeCalc
    }

    override fun unload(external: Boolean) {
        dump.destroyAll()
    }
}