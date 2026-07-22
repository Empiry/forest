package com.empire.forest.generator

import com.empire.forest.ForestContext
import com.empire.ignite.util.GlobalResourceTrackers
import com.empire.ignite.util.HACKS
import com.empire.ignite.util.UnloadableResource
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player

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

    companion object {
        private val GENERATOR_BLOCK_TYPE : Material = Material.RED_WOOL
    }

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


                    if (generatorDetectionHeuristic(player, genLoc, distanceSq)) {
                        return@filter true
                    }
//                    if (distanceSq < 4*4 ||
//                        (distanceSq < 75*75 && HACKS.lineOfSightGeneric(player, genLoc))
//                        || (distanceSq < 156*156 && player.hasLineOfSight(genLoc))) {
//                        return@filter true
//                    }
                }
                return@filter false
            }
            .map { (desc, _) -> desc }
        newlyUnlocked.forEach { desc -> unlocked[desc] = true }
    }

    private fun generatorDetectionHeuristic(player: Player, genLoc: Location, distanceSq: Double) : Boolean {
        if (distanceSq < 4*4) return true
        if (distanceSq < 75*75 && HACKS.lineOfSightGeneric(player, genLoc)) return true
        val hitResult = player.rayTraceBlocks(156.0)
        return (
            hitResult != null && hitResult.hitBlock != null &&
            hitResult.hitBlock!!.type == GENERATOR_BLOCK_TYPE &&
            genLoc.distanceSquared(hitResult.hitPosition.toLocation(genLoc.world)) < 4*4
        )
    }

    override fun unload(external: Boolean) {
        task?.unload()
        task = null
    }
}