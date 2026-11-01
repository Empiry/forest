package com.empire.forest.config

import com.empire.forest.ForestDataContainer
import com.empire.forest.ForestMapData
import com.empire.forest.GeneratorSelection
import com.empire.forest.gate.EscapeGateDescription
import com.empire.forest.generator.GeneratorDescription
import com.empire.ignite.util.location.RawLocation
import com.empire.ignite.util.region.AggregateRegion
import com.empire.ignite.util.region.CuboidRegion
import com.empire.ignite.util.region.RegionUtils
import com.empire.ignite.util.text.TextUtils.flattenComponents
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.util.Vector

private val DAMPENED_GENERATOR_CONTRIBUTION_MULTIPLIER: (Int) -> Double = { n -> (0.05 * (n - 1)) + 1 }
private val TEST_CONTRIBUTION_MULTIPLIER: (Int) -> Double = { n -> 1.8 }
private val FARM_SELECTION : GeneratorSelection = { allGenerators ->
    val shuffled = allGenerators.toMutableList().shuffled()
    val spillover = shuffled.drop(6)
    val generators = shuffled.take(6).toMutableList()
    // near the house area ish
    val houseGenerators = generators.filter { it.place.z in 521.0..524.0 }
    if (houseGenerators.size >= 2 && houseGenerators.any { gen -> gen.name == "Foyer" }) {
        generators.removeAll { it.name === "Foyer" }
        generators.add(spillover.random())
    }
    generators
}

object ForestStaticDataConfig {
    val MAIN_SERVER = ForestMapData(
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

    private val FARM_OVERWRITE_REGIONS = mapOf(
        "Garage" to CuboidRegion(Vector(-95, -4, 512), Vector(-96, -5, 514)),
        "Barn" to CuboidRegion(Vector(-100, -4, 550), Vector(-102, -5, 551)),
        "Cellar" to CuboidRegion(Vector(-139,-8,524), Vector(-140,-7,522)),
        "Foyer" to CuboidRegion(Vector(-137, -2, 523), Vector(-135, -3, 522)),
        "Upstairs" to CuboidRegion(Vector(-135,3,522), Vector(-134,4,524)),
        "Pit" to CuboidRegion(Vector(-167,-15,573), Vector(-169,-14,572)),
        "Storehouse" to CuboidRegion(Vector(-220, -2, 504), Vector(-218, -3, 505)),
        "Fields" to CuboidRegion(Vector(-188, 1, 449), Vector(-187, 0, 447))
    )

    private val MANOR_OVERWRITE_REGIONS = mapOf(
        "Balcony" to CuboidRegion(Vector(-478,73,1526), Vector(-477,72,1528)),
        "Vacant" to CuboidRegion(Vector(-442,73,1504), Vector(-440,72,1505)),
        "Library" to CuboidRegion(Vector(-447,73,1557), Vector(-449,72,1558)),
        "Kitchen" to CuboidRegion(Vector(-438,65,1559), Vector(-440,64,1560)),
        "Groundskeep" to CuboidRegion(Vector(-403,66,1611), Vector(-405,65,1612)),
        "Garden" to CuboidRegion(Vector(-452,65,1430), Vector(-451,64,1432)),
        "Hallway" to CuboidRegion(Vector(-432,65,1502), Vector(-431,64,1504)),
        "Cellar" to CuboidRegion(Vector(-448,57,1530), Vector(-449,56,1528))
    )

    // Balcony
    // -477.5 73.5 1527.5
    // -478 73 1526 -477 72 1528
    // rot 90

    // Vacant
    // -440.5 73.5 1505.5
    // -442 73 1504 -440 72 1505

    // Library
    // -447.5 73.5 1558.5
    // -447 73 1557 -449 72 1558

    // Kitchen
    // -438.5 65.5 1560.5
    // -438 65 1559 -440 64 1560

    // Groundskeep
    // -403.5 66.5 1612.5
    // -403 66 1611 -405 65 1612

    // Garden
    // -451.5 65.50 1431.5
    // -452 65 1430 -451 64 1432

    // Hallway
    // -431.5 65.5 1503.5
    // -432 65 1502 -431 64 1504

    // Cellar
    // -448 57 1530 -449 56 1528
    // -448.5 57.50 1529.5
    // rot 90

    // Hunter
    // -420 64 1524 0 0

    // Survivor
    // -503 63 1530 -509 63 1524
    // -506 63 1526 -90 0

    // spec -463 65 1527 -90 0

    fun getManor(debug: Boolean) = ForestMapData(
        mapName = "manor",
        minRequiredGeneratorsFn = { ctx ->
            if (ctx.survivorTeam.players.size >= 6) {
                6
            } else if (ctx.survivorTeam.players.size >= 4) {
                4
            } else {
                3
            }
        },
        generatorSelection = { allG ->
            val shuffled = allG.toMutableList().shuffled()
            val generators = shuffled.take(6).toMutableList()
            generators
        },
        queueCountdownSeconds = if (debug) 5 else 30,
        survivorReleaseSeconds = if (debug) 5 else 20,
        hunterReleaseSeconds = if (debug) 5 else 15,
        yLevelDeath = 30,
        worldPath = "./gamemaps/manor",
        survivorsSpawn = RawLocation(-506.0,63.0,1526.0,-90.0F,0.0F),
        survivorOrientation = -90.0F to 0.0F,
        huntersSpawn = RawLocation(-420.0,64.0,1524.0,0.0F,0.0F),
        survivorsRegion = CuboidRegion(
            Vector(-503.0,63.0,1530.0),
            Vector(-509.0,63.0,1524.0),
        ),
        spectatorsSpawn = RawLocation(-463.0,65.0,1527.0,-90.0F,0.0F),
        generators = listOf(
            GeneratorDescription(
                name = "Balcony",
                place = RawLocation(-477.5,73.5,1527.5),
                rotation = 90.0,
                unlockSeconds = 60
            ),
            GeneratorDescription(
                name = "Vacant",
                place = RawLocation(-440.5,73.5,1505.5),
                unlockSeconds = 60
            ),
            GeneratorDescription(
                name = "Library",
                place = RawLocation(-447.5,73.5,1558.5),
                unlockSeconds = 60
            ),
            GeneratorDescription(
                name = "Kitchen",
                place = RawLocation(-438.5,65.5,1560.5),
                unlockSeconds = 60
            ),
            GeneratorDescription(
                name = "Groundskeep",
                place = RawLocation(-403.5,66.5,1612.5),
                unlockSeconds = 60
            ),
            GeneratorDescription(
                name = "Garden",
                place = RawLocation(-451.5,65.50,1431.5),
                unlockSeconds = 60,
                rotation = 90.0
            ),
            GeneratorDescription(
                name = "Hallway",
                place = RawLocation(-431.5,65.5,1503.5),
                unlockSeconds = 60,
                rotation = 90.0
            ),
            GeneratorDescription(
                name = "Cellar",
                place = RawLocation(-448.5,57.50,1529.5),
                rotation = 90.0,
                unlockSeconds = 60
            )
        ),
        escape = EscapeGateDescription(
            region = AggregateRegion(listOf(
                CuboidRegion(
                    Vector(-387,65,1578),
                    Vector(-386,62,1586)
                ),
                CuboidRegion(
                    Vector(-408,67,1574),
                    Vector(-406,64,1573)
                )
            )),
            postEffect = { wrld ->
                RegionUtils.fillRegionReplacing(
                    CuboidRegion(
                            Vector(-387,65,1578),
                                Vector(-387,62,1586)
                    ),
                    wrld,
                    Material.AIR,
                    { b -> b.type == Material.POLISHED_DEEPSLATE_WALL }
                )
                RegionUtils.fillRegionReplacing(
                    CuboidRegion(
                        Vector(-408,67,1574),
                        Vector(-406,64,1574)
                    ),
                    wrld,
                    Material.AIR,
                    { b -> b.type == Material.POLISHED_DEEPSLATE_WALL }
                )
            }
        ),
        survivorSpawnBarrierRegion = CuboidRegion(
            Vector(0.0, 0.0, 0.0),
                Vector(0.0, 0.0, 0.0)
        ),
        hunterSpawnBarrierRegion = CuboidRegion(
            Vector(0.0, 0.0, 0.0),
            Vector(0.0, 0.0, 0.0)
        ),
        contributionMultiplier = DAMPENED_GENERATOR_CONTRIBUTION_MULTIPLIER,
        overwriteRegions = MANOR_OVERWRITE_REGIONS,
        credits =
            flattenComponents(
                Component.text("Redwood Manor").color(NamedTextColor.RED),
                Component.text(" by ").color(NamedTextColor.GRAY),
                Component.text("RyanDxxx").color(NamedTextColor.AQUA),
                Component.text(" and ").color(NamedTextColor.GRAY),
                Component.text("xykloptic").color(NamedTextColor.AQUA),
            )
    )

    val MAIN_SERVER_FARM = ForestMapData(
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
                place = RawLocation(-135.5,-1.5,522.5),
                unlockSeconds = 60
            ),
            GeneratorDescription(
                name = "Pit",
                place = RawLocation(-167.5,-13.5,572.5),
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
                place = RawLocation(-95.5,-3.5,513.5),
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
        overwriteRegions = FARM_OVERWRITE_REGIONS
    )

    fun getProdStaticData(debug: Boolean) : ForestDataContainer {
        return ForestDataContainer(
            mapOf(
                "Manor" to getManor(debug),
                "Farm" to getFarmStaticData(debug)
            )
        )
    }

    fun getFarmStaticData(debug: Boolean) : ForestMapData {
        return ForestMapData(
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
                    place = RawLocation(-135.5,-1.5,522.5),
                    unlockSeconds = 60
                ),
                GeneratorDescription(
                    name = "Pit",
                    place = RawLocation(-167.5,-13.5,573.5),
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
                    place = RawLocation(-95.5,-3.5,513.5),
                    unlockSeconds = 60,
                    rotation = 90.0
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
            overwriteRegions = FARM_OVERWRITE_REGIONS,
            credits =
                flattenComponents(
                    Component.text("Farm").color(NamedTextColor.YELLOW),
                    Component.text(" by ").color(NamedTextColor.GRAY),
                    Component.text("_Bill").color(NamedTextColor.YELLOW)
                )
        )
    }

    val TEST_SERVER_FARM = ForestMapData(
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
                place = RawLocation(-168.0,-14.0,572.5),
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

    val MY_TEST_SERVER_OLD = ForestMapData(
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

    val MY_TEST_SERVER = ForestMapData(
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