package com.empire.forest.config

import com.empire.forest.ForestDataContainer
import com.empire.forest.ForestMapData
import com.empire.ignite.util.location.RawLocation
import com.empire.ignite.util.region.CuboidRegion
import com.empire.ignite.util.region.Region
import org.bukkit.util.Vector

fun getProdServerConfig(debug: Boolean) = ServerConfig(
    forest = ForestStaticDataConfig.getProdStaticData(debug),
    lobby = LobbyConfig(
        lobbyWorld = "forestmapcopy",
        spawnRegion = CuboidRegion(
            Vector(658,60,1026),
            Vector(608,20,1156)
        ),
        spawnLocation = RawLocation(640.5,25.0,1095.5,0.0f, 0.0f),
        queueSignLocations = listOf(
            RawLocation(640.0,26.0,1106.0),
            RawLocation(646.0, 26.0, 1095.0),
            RawLocation(640.0, 26.0, 1084.0)
        )
    ),
    prod = !debug
)

val BUILD_SERVER_CONFIG = ServerConfig(
    forest = ForestStaticDataConfig.getProdStaticData(true),
    lobby = LobbyConfig(
        lobbyWorld = "forestmapcopy",
        spawnRegion = CuboidRegion(
            Vector(658,60,1026),
            Vector(608,20,1156)
        ),
        spawnLocation = RawLocation(640.5,25.0,1095.5,0.0f, 0.0f),
        queueSignLocations = listOf(RawLocation(640.0,26.0,1106.0))
    )
)

private val TEST_SERVER_CONFIG = ServerConfig(
    forest = ForestStaticDataConfig.getProdStaticData(true),
    lobby = LobbyConfig(
        lobbyWorld = "world",
        spawnRegion = CuboidRegion(
            Vector(592, 22, 343),
            Vector(329, -17, 127)
        ),
        spawnLocation = RawLocation(48.0, 71.0, 9.0),
        queueSignLocations = listOf(RawLocation(50.0, 72.0, 11.0))
    )
)

//val ACTIVE_SERVER_CONFIG : ServerConfig = TEST_SERVER_CONFIG



data class ServerConfig(
    val forest: ForestDataContainer,
    val lobby: LobbyConfig,
    val prod: Boolean = false
) {}

data class LobbyConfig(
    val lobbyWorld: String,
    val spawnRegion: Region,
    val spawnLocation: RawLocation,
    val queueSignLocations: List<RawLocation>
)


//private val LOBBY_WORLD_NAME = "world"
//private val SPAWN_REGION = CuboidRegion(
//    Vector(592, 22, 343),
//    Vector(329,-17,127)
//)
//private val SPAWN_LOCATION = RawLocation(48.0,71.0,9.0)