package com.empire.forest.generator

import com.empire.forest.ForestContext
import com.empire.ignite.util.GlobalResourceTrackers
import com.empire.ignite.util.HACKS
import com.empire.ignite.util.UnloadableResource

/**
 * Tracks whether survivors have "discovered" a generator
 */
class ForestGeneratorDiscovery(
    private val context: ForestContext,
    generators: List<Pair<GeneratorDescription, ForestGenerator>>
) : UnloadableResource {
    private var task : UnloadableResource? = null
    private val unlocked : MutableMap<GeneratorDescription, Boolean> = generators.associate {
        it.first to false
    }.toMutableMap()

    init {
        task = GlobalResourceTrackers.scheduler.repeatTask(10L, this::tick)
    }

    fun unlocked(generator: GeneratorDescription) : Boolean = unlocked[generator] ?: false

    private fun tick() {
        val newlyUnlocked = unlocked
            .filter { (_, v) -> !v  }
            .filter { (desc, _) ->
                for (player in context.survivorTeam.players) {
                    val genLoc = desc.place.toLocation(player.world)!!
                    val distanceSq = player.location.distanceSquared(genLoc)

                    if (distanceSq < 4*4 ||
                        (distanceSq < 156*156 && HACKS.lineOfSightGeneric(player, genLoc))) {
                        return@filter true
                    }
                }
                return@filter false
            }
            .map { (desc, _) -> desc }
        newlyUnlocked.forEach { desc -> unlocked[desc] = true }
    }

    override fun unload(external: Boolean) {
        task?.unload()
        task = null
    }
}