package com.empire.forest.util

import com.empire.forest.ForestContext
import com.empire.forest.config.BUILD_SERVER_CONFIG
import com.empire.forest.config.ServerConfig
import com.empire.forest.config.TEST_SERVER_CONFIG
import com.empire.forest.config.getProdServerConfig
import com.empire.forest.generator.GeneratorDescription
import com.empire.hacks.common.builder.UpdateEntityMetadataConverter
import com.empire.hacks.common.builder.UpdateEntityMetadataPacketBuilder
import com.empire.ignite.Ignite
import com.empire.ignite.util.GlobalResourceTrackers
import com.empire.ignite.util.HACKS
import com.empire.ignite.util.UnloadableResource
import com.empire.ignite.util.region.CuboidRegion
import com.empire.ignite.util.region.RegionUtils
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.io.File
import kotlin.experimental.or

object ForestUtils {
    fun <E: Entity> fakeGlow(recipients: List<Player>, targets: List<Entity>, duration: Long) {
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
            HACKS.overrideEntityMetadata(recipients, v)
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

    fun fakeGlowPersistent(recipients: List<Player>, targets: List<Entity>) : UnloadableResource {
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
            HACKS.overrideEntityMetadata(recipients, v)
        }

        // force-update before data-watcher kicks in
        recipients.forEach { r ->
            overridePackets.values.forEach {
                HACKS.sendDeltaUpdateEntityMetadataPacket(r, it)
            }
        }

        return object : UnloadableResource {
            override fun unload(external: Boolean) {
                unloadables.forEach { it.release() }
                recipients.forEach { r ->
                    resetPackets.forEach { rp ->
                        HACKS.sendDeltaUpdateEntityMetadataPacket(r, rp)
                    }
                }
            }

        }
    }

    fun replacePlaceholderBlocks(desc: GeneratorDescription, generatorSource: Location) {
        val broadRegion = CuboidRegion(
            Vector(generatorSource.x + 3, generatorSource.y + 3, generatorSource.z + 3),
            Vector(generatorSource.x - 3, generatorSource.y - 3, generatorSource.z - 3)
        )
        RegionUtils.fillRegionReplacing(
            broadRegion, generatorSource.world, Material.AIR, { it.type == Material.RED_TERRACOTTA }
        )
//        val innerRegion = CuboidRegion(
//            generatorSource.clone()
//                .add(desc.getLengthDir().clone().multiply(-1.0))
//                .add(Vector(0, -1, 0)).toVector(),
//            generatorSource.clone()
//                .add(desc.getLengthDir().clone().multiply(1.0))
//                .add(desc.getWidthDir().clone().multiply(1.0)).toVector(),
//        )
//        RegionUtils.fillRegion(
//            innerRegion, generatorSource.world, Material.BARRIER
//        )
    }


    fun freezePlayer(player: Player) {
        player.getAttribute(Attribute.MOVEMENT_SPEED)?.let { it.baseValue = 0.0 }
        player.getAttribute(Attribute.JUMP_STRENGTH)?.let { it.baseValue = 0.0 }
    }

    fun resetPlayer(context: ForestContext, player: Player) {
        player.getAttribute(Attribute.MOVEMENT_SPEED)?.let { it.baseValue = context.getNormalMovementSpeed(player) }
        player.getAttribute(Attribute.JUMP_STRENGTH)?.let { it.baseValue = it.defaultValue }
    }

    fun loadBasicConfig(plugin: Ignite, baseDirectory: File) : ServerConfig {
        val config = File(baseDirectory, "forest.yml")

        if (!config.exists()) {
            config.parentFile.mkdirs()
        }
        val yamlConfig = YamlConfiguration.loadConfiguration(config);
        val staticConfigName = yamlConfig.getString("static-config-name", "prod")
        if (staticConfigName == "prod") {
            return getProdServerConfig(false)
        } else if (staticConfigName == "prod-debug") {
            return getProdServerConfig(true)
        } else if (staticConfigName == "test") {
            return TEST_SERVER_CONFIG
        } else {
            return BUILD_SERVER_CONFIG
        }
    }
}

data class ForestAppConfig(
    val staticConfigName: String
)