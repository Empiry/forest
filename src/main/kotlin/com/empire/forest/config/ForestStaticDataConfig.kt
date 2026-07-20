package com.empire.forest.config

import com.empire.forest.ForestStaticData
import com.empire.forest.gate.EscapeGateDescription
import com.empire.forest.generator.GeneratorDescription
import com.empire.ignite.util.location.RawLocation
import com.empire.ignite.util.region.CuboidRegion
import org.bukkit.util.Vector

object ForestStaticDataConfig {
    val MAIN_SERVER = ForestStaticData(
        queueCountdownSeconds = 20,
        survivorReleaseSeconds = 20,
        hunterReleaseSeconds = 35,
        yLevelDeath = -40,
        worldPath = "./gamemaps/forestmapcopy",
        RawLocation(-187.0,-16.0,44.0,0.0F,0.0F),
        RawLocation(-82.0, -17.0, 232.0),
        generators = listOf(
            GeneratorDescription(
                name = "Dock",
                place = RawLocation(-103.0,-22.0,80.0),
                unlockSeconds = 12
            ),
            GeneratorDescription(
                name = "Bridge",
                place = RawLocation(-155.0,-16.0,172.0),
                unlockSeconds = 12
            ),
            GeneratorDescription(
                name = "UnnamedArea",
                place = RawLocation(-202.0,-18.0,181.0),
                unlockSeconds = 12
            ),
            GeneratorDescription(
                name = "House",
                place = RawLocation(-107.0,-12.0,148.0),
                unlockSeconds = 12
            ),
            GeneratorDescription(
                name = "Garage",
                place = RawLocation(-105.0,-18.0,149.0),
                unlockSeconds = 12
            ),
            GeneratorDescription(
                name = "HouseNumeroDos",
                place = RawLocation(-70.0,-17.0,95.0),
                unlockSeconds = 12
            )
        ),
        escape = EscapeGateDescription(
            location = RawLocation(-54.0, -15.0, 193.0),
            radius = 2
        ),
        survivorSpawnBarrierRegion = CuboidRegion(
            Vector(-187, -16,48),
            Vector(-186, -14, 48)
        ),
        hunterSpawnBarrierRegion = CuboidRegion(
            Vector(-78, -14, 228),
            Vector(-85, -19, 228)
        ),
    )

    val MY_TEST_SERVER_OLD = ForestStaticData(
        queueCountdownSeconds = 3,
        survivorReleaseSeconds = 4,
        hunterReleaseSeconds = 7,
        yLevelDeath = -10,
        worldPath = "./gamemaps/forestmapcopy",
        survivorsSpawn = RawLocation(-150.0, 10.0, 89.0, 0.0F, 0.0F),
        huntersSpawn = RawLocation(-46.0, 9.0, 276.0, -180.0F, 0.0F),
        generators =
        listOf(
            GeneratorDescription(
                "Dock",
                RawLocation(-66.0, 4.0, 125.0),
                12
            )
        ),
        escape = EscapeGateDescription(
            RawLocation(-18.0, 11.0, 238.0), 1
        ),
        survivorSpawnBarrierRegion = CuboidRegion(
            Vector(-151, 10, 93),
            Vector(-150, 12, 93)
        ),
        hunterSpawnBarrierRegion = CuboidRegion(
            Vector(-151, 10, 93),
            Vector(-150, 12, 93)
        )
    )

    val MY_TEST_SERVER = ForestStaticData(
        queueCountdownSeconds = 3,
        survivorReleaseSeconds = 4,
        hunterReleaseSeconds = 7,
        yLevelDeath = -40,
        worldPath = "./gamemaps/forestmapcopy",
        RawLocation(-187.0,-16.0,44.0,0.0F,0.0F),
        RawLocation(-82.0, -17.0, 232.0),
        generators = listOf(
            GeneratorDescription(
                name = "Dock",
                place = RawLocation(-103.0,-22.0,80.0),
                unlockSeconds = 12
            ),
            GeneratorDescription(
                name = "Bridge",
                place = RawLocation(-155.0,-16.0,172.0),
                unlockSeconds = 12
            ),
            GeneratorDescription(
                name = "UnnamedArea",
                place = RawLocation(-202.0,-18.0,181.0),
                unlockSeconds = 12
            ),
            GeneratorDescription(
                name = "House",
                place = RawLocation(-107.0,-12.0,148.0),
                unlockSeconds = 12
            ),
            GeneratorDescription(
                name = "Garage",
                place = RawLocation(-105.0,-18.0,149.0),
                unlockSeconds = 12
            ),
            GeneratorDescription(
                name = "HouseNumeroDos",
                place = RawLocation(-70.0,-17.0,95.0),
                unlockSeconds = 12
            )
        ),
        escape = EscapeGateDescription(
            location = RawLocation(-54.0, -15.0, 193.0),
            radius = 2
        ),
        survivorSpawnBarrierRegion = CuboidRegion(
            Vector(-187, -16,48),
            Vector(-186, -14, 48)
        ),
        hunterSpawnBarrierRegion = CuboidRegion(
            Vector(-78, -14, 228),
            Vector(-85, -19, 228)
        ),
    )
}

data class ForestInteractiveData(val isDebug: Boolean) {

}