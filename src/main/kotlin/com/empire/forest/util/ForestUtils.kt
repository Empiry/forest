package com.empire.forest.util

import com.empire.hacks.common.builder.UpdateEntityMetadataConverter
import com.empire.hacks.common.builder.UpdateEntityMetadataPacketBuilder
import com.empire.ignite.util.GlobalResourceTrackers
import com.empire.ignite.util.HACKS
import com.empire.ignite.util.region.CuboidRegion
import com.empire.ignite.util.region.RegionUtils
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.collections.forEach
import kotlin.experimental.or

object ForestUtils {
    fun fakeGlow(recipients: List<Player>, targets: List<Player>, duration: Long) {
        val glowByte : Byte = 0x40
        val overridePackets = targets.associateWith {
            target ->
                UpdateEntityMetadataPacketBuilder(target, listOf(
                    UpdateEntityMetadataConverter.MainEntityByteMask { mask ->
                        mask or glowByte
                    }
                ))
        }
        val resetPackets = targets.map { target ->
            UpdateEntityMetadataPacketBuilder(target, listOf(
                UpdateEntityMetadataConverter.MainEntityByteMask { mask -> mask }
            ))
        }

        val unloadables = overridePackets.map { (k, v) ->
            HACKS.overrideEntityMetadata(recipients, k, v)
        }

        // force-update before data-watcher kicks in
        recipients.forEach { r ->
            overridePackets.values.forEach {
                HACKS.sendDeltaUpdateEntityMetadataPacket(r, it)
            }
        }

        GlobalResourceTrackers.scheduler.after(duration) {
            unloadables.forEach { it.release() }
            recipients.forEach { r ->
                resetPackets.forEach { rp ->
                    HACKS.sendDeltaUpdateEntityMetadataPacket(r, rp)
                }
            }
        }
    }

    fun replacePlaceholderBlocks(generatorSource: Location) {
        val broadRegion = CuboidRegion(
            Vector(generatorSource.x + 3, generatorSource.y + 3, generatorSource.z + 3),
            Vector(generatorSource.x - 3, generatorSource.y - 3, generatorSource.z - 3)
        )
        RegionUtils.fillRegionReplacing(
            broadRegion, generatorSource.world, Material.AIR, { it.type == Material.RED_TERRACOTTA }
        )
//        val innerRegion = CuboidRegion(
//            Vector(generatorSource.x + 1, generatorSource.y - 1, generatorSource.z),
//            Vector(generatorSource.x - 1, generatorSource.y, generatorSource.z - 1)
//        )
//        RegionUtils.fillRegion(
//            innerRegion, generatorSource.world, Material.BARRIER
//        )
    }
}