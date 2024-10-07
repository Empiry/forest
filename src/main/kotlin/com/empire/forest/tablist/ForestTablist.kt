package com.empire.forest.tablist

import com.empire.forest.ForestContext
import com.empire.forest.generator.ForestGenerator
import com.empire.forest.generator.GeneratorDescription
import com.empire.forest.util.ForestMessaging
import com.empire.hacks.common.skins.Skins
import com.empire.hacks.common.tab.TabSlot
import com.empire.hacks.common.tab.TablistEntry
import com.empire.ignite.util.GlobalResourceTrackers
import com.empire.ignite.util.IgniteResource
import com.empire.ignite.util.ManagedResourceDump
import com.empire.ignite.util.callback.ForwardCallbackRegistration
import com.empire.ignite.util.callback.PassthroughFilterCallback
import com.empire.ignite.util.tab.IgniteTablist
import com.empire.ignite.util.tab.TabSection
import net.kyori.adventure.text.Component
import org.bukkit.plugin.java.JavaPlugin
import kotlin.math.floor

class ForestTablist(
    private val plugin: JavaPlugin,
    private val forestContext: ForestContext
) : IgniteResource {
    private val tablist: IgniteTablist = IgniteTablist(forestContext.playerTracker, forestContext.messageBus)

    private val generatorProgresses : MutableList<Pair<GeneratorDescription, ForestGenerator>> = mutableListOf()
    private var dump : ManagedResourceDump? = null
    private lateinit var generatorTablistPushUpdate: () -> Unit

    override fun load() {
        tablist.load()
        tablist.addTeamSection(forestContext.survivorTeam, TabSlot(0, 0), TabSection(0, 1, 0, 16))
        tablist.addTeamSection(forestContext.hunterTeam, TabSlot(1, 0), TabSection(1, 1, 1, 16))
        generatorTablistPushUpdate = tablist.addGenericCollectionTabSection(
            TabSection(0, 18, 3, 19),
            generatorProgresses,
            { (generatorDesc, generator) ->
               val generatorProgress = floor(
                   (generator.progressTicks.toDouble() / (generatorDesc.unlockSeconds * 20)) * 100.0
               ).toInt()
               val progressColor = ForestMessaging.getGeneratorProgressColor(generatorProgress)
               TablistEntry(
                   displayName = Component.text("${generatorDesc.name} (${generatorProgress}%)")
                       .color(progressColor),
                   skin = Skins.GRAY_SQUARE,
                   latency = TablistEntry.latencyBarsAsValue(when (generatorProgress) {
                       in 0..25 -> 1
                       in 26..50 -> 2
                       in 51..75 -> 3
                       in 76..99 -> 4
                       else -> 5
                   }),
                   gameMode = null
               )
            }
        )
    }

    fun registerGeneratorProgressUpdate(
        generators: List<Pair<GeneratorDescription, ForestGenerator>>,
        generatorProgressUpdate: ForwardCallbackRegistration<Unit>,
        parentDump: ManagedResourceDump
    ) {
        val childDump = GlobalResourceTrackers.createResourceDumpWithParent(parentDump)
        this.dump = childDump
        this.generatorProgresses += generators
        generatorProgressUpdate.register(
            PassthroughFilterCallback.createActiveUsingTicks(plugin, 10L, childDump) {
                generatorTablistPushUpdate()
            }
        )
        generatorTablistPushUpdate()
    }

    override fun unload(external: Boolean) {
        tablist.unload()
        dump?.destroyAll()
        dump = null
    }
}