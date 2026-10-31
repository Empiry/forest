package com.empire.forest.config

import com.empire.forest.ForestStaticData
import com.empire.forest.GeneratorSelection
import com.empire.forest.gate.EscapeGateDescription
import com.empire.forest.generator.GeneratorDescription
import com.empire.ignite.util.location.RawLocation
import com.empire.ignite.util.region.AggregateRegion
import com.empire.ignite.util.region.CuboidRegion
import org.bukkit.util.Vector

private val DAMPENED_GENERATOR_CONTRIBUTION_MULTIPLIER: (Int) -> Double = { n -> (0.05 * (n - 1)) + 1 }
private val TEST_CONTRIBUTION_MULTIPLIER: (Int) -> Double = { n -> 1.8 }
private val FARM_SELECTION : GeneratorSelection = { allGenerators ->
    val shuffled = allGenerators.toMutableList().shuffled()
    val spillover = shuffled.drop(7)
    val generators = shuffled.take(7).toMutableList()
    // near the house area ish
    val houseGenerators = generators.filter { it.place.z in 521.0..524.0 }
    if (houseGenerators.size >= 2 && houseGenerators.any { gen -> gen.name == "Foyer" }) {
        generators.removeAll { it.name === "Foyer" }
        generators.add(spillover.random())
    }
    generators
}

object ForestStaticDataConfig {
    val MAIN_SERVER = ForestStaticData(
        mapName = null,
        queueCountdownSeconds = 20,
        survivorReleaseSeconds = 20,
        hunterReleaseSeconds = 35,
        yLevelDeath = -40,
        worldPath = "./gamemaps/forestmapcopy",
        survivorsSpawn = RawLocation(-187.0,-16.0,44.0,0.0F,0.0F),
        huntersSpawn = RawLocation(-82.0, -17.0, 232.0, 180f, 0f),
        spectatorsSpawn = RawLocation(-142.0, 8.0, 152.0, -53f, 37f),
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
            region =
                AggregateRegion(listOf(
                    CuboidRegion(
                        Vector(-54,-15,191),
                        Vector(-53,-14,195),
                    ),
                    CuboidRegion(
                        Vector(-49, -14, 156),
                        Vector(-48, -13, 158),
                    )
                ))
        ),
        survivorSpawnBarrierRegion = CuboidRegion(
            Vector(-187, -16,48),
            Vector(-186, -14, 48)
        ),
        hunterSpawnBarrierRegion = CuboidRegion(
            Vector(-78, -14, 228),
            Vector(-85, -19, 228)
        ),
        contributionMultiplier = DAMPENED_GENERATOR_CONTRIBUTION_MULTIPLIER
    )

    val mainServerDebug = true
    val MAIN_SERVER_FARM = ForestStaticData(
        mapName = "farm",
        minRequiredGeneratorsFn = { ctx ->
            if (ctx.survivorTeam.players.size >= 6) {
                6
            } else if (ctx.survivorTeam.players.size >= 4) {
                4
            } else {
                3
            }
        },
        generatorSelection = FARM_SELECTION,
        queueCountdownSeconds = if (mainServerDebug) 10 else 30,
        survivorReleaseSeconds = if (mainServerDebug) 15 else 20,
        hunterReleaseSeconds = if (mainServerDebug) 10 else 15,
        yLevelDeath = -26,
        worldPath = "./gamemaps/farm",
        survivorsSpawn = RawLocation(-109.0,-1.0,423.0,0.0F,0.0F),
        survivorsRegion = CuboidRegion(
            Vector(-106.0,-1.0,421.0),
            Vector(-111.0,-1.0,427.0)
        ),
        huntersSpawn = RawLocation(-216.0,-12.0,581.0,-134.7f,-34.4f),
        spectatorsSpawn = RawLocation(-188.0,10.0,464.0,-31.9f,33.4f),
        generators = listOf(
            GeneratorDescription(
                name = "Fields",
                place = RawLocation( -187.5, 1.5, 448.5),
                unlockSeconds = 60,
                rotation = 90.0
            ),
            GeneratorDescription(
                name = "Storehouse",
                place = RawLocation(-218.5,-1.5,505.5),
                unlockSeconds = 60
            ),
            GeneratorDescription(
                name = "Barn",
                place = RawLocation(-100.5,-3.5,551.5),
                unlockSeconds = 60
            ),
            GeneratorDescription(
                name = "Upstairs",
                place = RawLocation(-134.5,4.5,523.5),
                unlockSeconds = 60,
                rotation = 90.0
            ),
            GeneratorDescription(
                name = "Foyer",
                place = RawLocation(-135.5,-2.0,522.5),
                unlockSeconds = 60
            ),
            GeneratorDescription(
                name = "Pit",
                place = RawLocation(-167.5,-14.0,572.5),
                unlockSeconds = 60
            ),
            GeneratorDescription(
                name = "Cellar",
                place = RawLocation(-139.5,-6.5,523.5),
                unlockSeconds = 60,
                rotation = 90.0
            ),
            GeneratorDescription(
                name = "Garage",
                place = RawLocation(-95.5,-4.0,513.5),
                unlockSeconds = 60
            )
        ),
        escape = EscapeGateDescription(
            region =
                AggregateRegion(listOf(
                    CuboidRegion(
                        Vector(-117,-1,419),
                        Vector(-116,1,420),
                    ),
                    CuboidRegion(
                        Vector(-100,1,425),
                        Vector(-99,3,426),
                    )
                ))
        ),
        survivorSpawnBarrierRegion = CuboidRegion(
            Vector(-107,-1,421),
            Vector(-111,2,425)
        ),
        hunterSpawnBarrierRegion = CuboidRegion(
            Vector(-213,-1,448),
            Vector( -214,-3,444)
        ),
        contributionMultiplier = DAMPENED_GENERATOR_CONTRIBUTION_MULTIPLIER,
        overwriteRegions = listOf(
            // Garage
            CuboidRegion(Vector(-95, -3, 512), Vector(-96, -5, 514)),
            // Barn
            CuboidRegion(Vector(-100, -4, 550), Vector(-102, -5, 551)),
            // Cellar
            CuboidRegion(Vector(-139,-8,524), Vector(-140,-7,522)),
            // Foyer
            CuboidRegion(Vector(-137, -2, 523), Vector(-135, -3, 522)),
            // Upstairs
            CuboidRegion(Vector(-135,3,522), Vector(-134,4,524)),
            // Pit
            CuboidRegion(Vector(-167, -13, 573), Vector(-169, -15, 572)),
            // Storehouse
            CuboidRegion(Vector(-220, -2, 504), Vector(-218, -3, 505)),
            // Fields
            CuboidRegion(Vector(-188, 1, 449), Vector(-187, 0, 447)),
        )
    )

    fun getProdStaticData(debug: Boolean) : ForestStaticData {
        return ForestStaticData(
            debugLever = debug,
            mapName = "farm",
            minRequiredQueue = if (debug) 1 else 2,
            minRequiredGeneratorsFn = { ctx ->
                if (ctx.survivorTeam.players.size >= 6) {
                    6
                } else if (ctx.survivorTeam.players.size >= 4) {
                    4
                } else {
                    3
                }
            },
            generatorSelection = FARM_SELECTION,
            queueCountdownSeconds = if (debug) 5 else 30,
            survivorReleaseSeconds = if (debug) 5 else 20,
            hunterReleaseSeconds = if (debug) 4 else 15,
            yLevelDeath = -26,
            worldPath = "./gamemaps/farm",
            survivorsSpawn = RawLocation(-109.0,-1.0,423.0,0.0F,0.0F),
            survivorsRegion = CuboidRegion(
                Vector(-106.0,-1.0,421.0),
                Vector(-111.0,-1.0,427.0)
            ),
            huntersSpawn = RawLocation(-216.0,-12.0,581.0,-134.7f,-34.4f),
            spectatorsSpawn = RawLocation(-188.0,10.0,464.0,-31.9f,33.4f),
            generators = listOf(
                GeneratorDescription(
                    name = "Fields",
                    place = RawLocation( -187.5, 1.5, 448.5),
                    unlockSeconds = 60,
                    rotation = 90.0
                ),
                GeneratorDescription(
                    name = "Storehouse",
                    place = RawLocation(-218.5,-1.5,505.5),
                    unlockSeconds = 60
                ),
                GeneratorDescription(
                    name = "Barn",
                    place = RawLocation(-100.5,-3.5,551.5),
                    unlockSeconds = 60
                ),
                GeneratorDescription(
                    name = "Upstairs",
                    place = RawLocation(-134.5,4.5,523.5),
                    unlockSeconds = 60,
                    rotation = 90.0
                ),
                GeneratorDescription(
                    name = "Foyer",
                    place = RawLocation(-135.5,-2.0,522.5),
                    unlockSeconds = 60
                ),
                GeneratorDescription(
                    name = "Pit",
                    place = RawLocation(-167.5,-14.0,572.5),
                    unlockSeconds = 60
                ),
                GeneratorDescription(
                    name = "Cellar",
                    place = RawLocation(-139.5,-6.5,523.5),
                    unlockSeconds = 60,
                    rotation = 90.0
                ),
                GeneratorDescription(
                    name = "Garage",
                    place = RawLocation(-95.5,-4.0,513.5),
                    unlockSeconds = 60
                )
            ),
            escape = EscapeGateDescription(
                region =
                    AggregateRegion(listOf(
                        CuboidRegion(
                            Vector(-117,-1,419),
                            Vector(-116,1,420),
                        ),
                        CuboidRegion(
                            Vector(-100,1,425),
                            Vector(-99,3,426),
                        )
                    ))
            ),
            survivorSpawnBarrierRegion = CuboidRegion(
                Vector(-107,-1,421),
                Vector(-111,2,425)
            ),
            hunterSpawnBarrierRegion = CuboidRegion(
                Vector(-213,-1,448),
                Vector( -214,-3,444)
            ),
            contributionMultiplier = DAMPENED_GENERATOR_CONTRIBUTION_MULTIPLIER,
            overwriteRegions = listOf(
                // Garage
                CuboidRegion(Vector(-95, -3, 512), Vector(-96, -5, 514)),
                // Barn
                CuboidRegion(Vector(-100, -4, 550), Vector(-102, -5, 551)),
                // Cellar
                CuboidRegion(Vector(-139,-8,524), Vector(-140,-7,522)),
                // Foyer
                CuboidRegion(Vector(-137, -2, 523), Vector(-135, -3, 522)),
                // Upstairs
                CuboidRegion(Vector(-135,3,522), Vector(-134,4,524)),
                // Pit
                CuboidRegion(Vector(-167, -13, 573), Vector(-169, -15, 572)),
                // Storehouse
                CuboidRegion(Vector(-220, -2, 504), Vector(-218, -3, 505)),
                // Fields
                CuboidRegion(Vector(-188, 1, 449), Vector(-187, 0, 447)),
            )
        )
    }

    val TEST_SERVER_FARM = ForestStaticData(
        mapName = "farm",
        minRequiredGeneratorsFn = { ctx ->
            if (ctx.survivorTeam.players.size >= 9) {
                5
            } else if (ctx.survivorTeam.players.size >= 5) {
                4
            } else {
                3
            }
        },
        generatorSelection = FARM_SELECTION,
        queueCountdownSeconds = 5,
        survivorReleaseSeconds = 4,
        hunterReleaseSeconds = 5,
        yLevelDeath = -26,
        worldPath = "./gamemaps/farm",
        survivorsSpawn = RawLocation(-109.0,-1.0,423.0,0.0F,0.0F),
        survivorsRegion = CuboidRegion(
            Vector(-106.0,-1.0,421.0),
            Vector(-111.0,-1.0,427.0)
        ),
        huntersSpawn = RawLocation(-216.0,-3.0,446.0, -90f, 0f),
        spectatorsSpawn = RawLocation(-188.0,10.0,464.0,-31.9f,33.4f),
        generators = listOf(
            GeneratorDescription(
                name = "Fields",
                place = RawLocation(-187.0, 1.0, 448.0),
                unlockSeconds = 12
            ),
            GeneratorDescription(
                name = "Storehouse",
                place = RawLocation(-219.0, -2.0, 505.0),
                unlockSeconds = 12
            ),
            GeneratorDescription(
                name = "Barn",
                place = RawLocation(-101.0, -4.0, 551.0),
                unlockSeconds = 12
            ),
            GeneratorDescription(
                name = "Upstairs",
                place = RawLocation( -134.0,4.0,523.0),
                unlockSeconds = 12
            ),
            GeneratorDescription(
                name = "Foyer",
                place = RawLocation(-136.0,-2.0,522.0),
                unlockSeconds = 12
            ),
            GeneratorDescription(
                name = "Pit",
                place = RawLocation(-168.0,-14.0,572.0),
                unlockSeconds = 12
            ),
            GeneratorDescription(
                name = "Cellar",
                place = RawLocation(-139.0,-7.0,523.0),
                unlockSeconds = 12
            ),
            GeneratorDescription(
                name = "Garage",
                place = RawLocation(-96.0,-4.0,513.0),
                unlockSeconds = 12
            )
        ),
        escape = EscapeGateDescription(
            region =
                AggregateRegion(listOf(
                    CuboidRegion(
                        Vector(-117,-1,419),
                        Vector(-116,1,420),
                    ),
                    CuboidRegion(
                        Vector(-100,1,429),
                        Vector(-99,3,430),
                    )
                ))
        ),
        survivorSpawnBarrierRegion = CuboidRegion(
            Vector(-107,-1,421),
            Vector(-111,2,425)
        ),
        hunterSpawnBarrierRegion = CuboidRegion(
            Vector(-213,-1,448),
            Vector( -214,-3,444)
        ),
        contributionMultiplier = DAMPENED_GENERATOR_CONTRIBUTION_MULTIPLIER
    )

    val MY_TEST_SERVER_OLD = ForestStaticData(
        mapName = null,
        queueCountdownSeconds = 3,
        survivorReleaseSeconds = 4,
        hunterReleaseSeconds = 7,
        yLevelDeath = -10,
        worldPath = "./gamemaps/forestmapcopy",
        survivorsSpawn = RawLocation(-150.0, 10.0, 89.0, 0.0F, 0.0F),
        huntersSpawn = RawLocation(-46.0, 9.0, 276.0, -180.0F, 0.0F),
        spectatorsSpawn = RawLocation(-142.0, 8.0, 152.0, -53f, 37f),
        generators =
        listOf(
            GeneratorDescription(
                "Dock",
                RawLocation(-66.0, 4.0, 125.0),
                12
            )
        ),
        escape = EscapeGateDescription(
            region =
                AggregateRegion(listOf(
                    CuboidRegion(
                        Vector(-54,-15,191),
                        Vector(-53,-14,195),
                    ),
                    CuboidRegion(
                        Vector(-49, -14, 156),
                        Vector(-48, -13, 158),
                    )
                ))
        ),
        survivorSpawnBarrierRegion = CuboidRegion(
            Vector(-151, 10, 93),
            Vector(-150, 12, 93)
        ),
        hunterSpawnBarrierRegion = CuboidRegion(
            Vector(-151, 10, 93),
            Vector(-150, 12, 93)
        ),
        contributionMultiplier = TEST_CONTRIBUTION_MULTIPLIER
    )

    val MY_TEST_SERVER = ForestStaticData(
        mapName = null,
        queueCountdownSeconds = 3,
        survivorReleaseSeconds = 4,
        hunterReleaseSeconds = 7,
        yLevelDeath = -40,
        worldPath = "./gamemaps/forestmapcopy",
        survivorsSpawn = RawLocation(-187.0,-16.0,44.0,0.0F,0.0F),
        huntersSpawn = RawLocation(-82.0, -17.0, 232.0, 180f, 0f),
        spectatorsSpawn = RawLocation(-142.0, 8.0, 152.0, -53f, 37f),
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
            region =
                AggregateRegion(listOf(
                    CuboidRegion(
                        Vector(-54,-15,191),
                        Vector(-53,-14,195),
                    ),
                    CuboidRegion(
                        Vector(-49, -14, 156),
                        Vector(-48, -13, 158),
                    )
                ))
        ),
        survivorSpawnBarrierRegion = CuboidRegion(
            Vector(-187, -16,48),
            Vector(-186, -14, 48)
        ),
        hunterSpawnBarrierRegion = CuboidRegion(
            Vector(-78, -14, 228),
            Vector(-85, -19, 228)
        ),
        contributionMultiplier = DAMPENED_GENERATOR_CONTRIBUTION_MULTIPLIER
    )
}

data class ForestInteractiveData(val isDebug: Boolean) {

}

data class ForestConfigurableData(
    val queueCountdownSeconds: Int,
    val survivorReleaseSeconds: Int,
    val hunterReleaseSeconds: Int
)