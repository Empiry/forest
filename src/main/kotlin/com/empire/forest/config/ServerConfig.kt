package com.empire.forest.config

import com.empire.forest.ForestStaticData
import com.empire.ignite.util.location.RawLocation
import com.empire.ignite.util.region.CuboidRegion
import com.empire.ignite.util.region.Region
import org.bukkit.util.Vector

private val MAIN_SERVER_CONFIG = ServerConfig(
    forest = ForestStaticDataConfig.MAIN_SERVER_FARM,
    lobby = LobbyConfig(
        lobbyWorld = "oldworldgenzio",
        spawnRegion = CuboidRegion(
            Vector(592, 22, 343),
            Vector(329, -17, 127)
        ),
        spawnLocation = RawLocation(400.0, -16.0, 307.0, 180.0f, 0.0f),
        queueSignLoc = RawLocation(400.0, -15.0, 285.0)
    )
)

private val TEST_SERVER_CONFIG = ServerConfig(
    forest = ForestStaticDataConfig.TEST_SERVER_FARM,
    lobby = LobbyConfig(
        lobbyWorld = "world",
        spawnRegion = CuboidRegion(
            Vector(592, 22, 343),
            Vector(329, -17, 127)
        ),
        spawnLocation = RawLocation(48.0, 71.0, 9.0),
        queueSignLoc = RawLocation(50.0, 72.0, 11.0)
    )
)

val ACTIVE_SERVER_CONFIG : ServerConfig = TEST_SERVER_CONFIG

data class ServerConfig(
    val forest: ForestStaticData,
    val lobby: LobbyConfig
) {}

data class LobbyConfig(
    val lobbyWorld: String,
    val spawnRegion: Region,
    val spawnLocation: RawLocation,
    val queueSignLoc: RawLocation
)


//private val LOBBY_WORLD_NAME = "world"
//private val SPAWN_REGION = CuboidRegion(
//    Vector(592, 22, 343),
//    Vector(329,-17,127)
//)
//private val SPAWN_LOCATION = RawLocation(48.0,71.0,9.0)