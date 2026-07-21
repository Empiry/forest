package com.empire.forest.data

import com.empire.ignite.storage.*
import com.empire.ignite.util.UnloadableResource
import java.util.*

data class ForestUserData(val coins: Int)

class ForestUserDatabase(
    val access: CacheAndDatabaseResourceAccess<UUID, ForestUserData>,
    private val storage: IgniteSQLStorage<*>
) : UnloadableResource {
    override fun unload(external: Boolean) {
        access.flushAll()
        storage.unload()
    }
}

object ForestUserDatabaseProvider {
    private val TABLE_DEFINITION =
        """
            CREATE TABLE IF NOT EXISTS forest_user_data (
                player VARCHAR(36) NOT NULL PRIMARY KEY, coins INTEGER NOT NULL
            )
        """.trimIndent()

    private val GET_PLAYER_DATA_STATEMENT =
        """
            SELECT coins FROM forest_user_data WHERE player = ?
        """.trimIndent()

    private val INSERT_PLAYER_DATA_STATEMENT =
        """
            INSERT INTO forest_user_data (player, coins) 
            VALUES (?, ?)
        """.trimIndent()

    private val UPDATE_PLAYER_DATA_STATEMENT =
        """
            UPDATE forest_user_data SET coins = ? WHERE player = ?
        """.trimIndent()

    fun create(path: String) : ForestUserDatabase {
        val persistentData = SQLiteStorage()
        persistentData.initialize(SQLiteConfig(path))
        persistentData.connection.createStatement().execute(TABLE_DEFINITION)
        val db = object : KeyedResourceAccess<UUID, ForestUserData> {
            private fun getUserData(player: UUID): ForestUserData =
                persistentData.sql().executeQueryStatement(
                    GET_PLAYER_DATA_STATEMENT,
                    SQLExecutionHelper.StatementExecutionCallback.QueryStatement(
                            configure = { stmt ->
                                stmt.setString(1, player.toString())
                            },
                            handler = { rs ->
                                if (!rs.next()) {
                                    setDefaultData(player)
                                    ForestUserData(0)
                                } else {
                                    ForestUserData(rs.getInt("coins"))
                                }
                            }
                    )
                )

            private fun setUserData(player: UUID, data: ForestUserData) {
                getUserData(player) // force load if not present
                persistentData.sql().executeDMLStatement(
                    UPDATE_PLAYER_DATA_STATEMENT,
                    SQLExecutionHelper.StatementExecutionCallback.DMLStatement(
                        configure = { stmt ->
                            stmt.setInt(1, data.coins)
                            stmt.setString(2, player.toString())
                        },
                        handler = { }
                    )
                )
            }

            private fun setDefaultData(player: UUID) {
                persistentData.sql().executeDMLStatement(
                    INSERT_PLAYER_DATA_STATEMENT,
                    SQLExecutionHelper.StatementExecutionCallback.DMLStatement(
                        configure = { stmt ->
                            stmt.setString(1, player.toString())
                            stmt.setInt(2, 0)
                        },
                        handler = { }
                    )
                )
            }

            override fun getResource(key: UUID): ForestUserData? = getUserData(key)

            override fun updateResource(key: UUID, resource: ForestUserData) {
                setUserData(key, resource)
            }
        }
        val access = CacheAndDatabaseResourceAccess(
            MapResourceCache(),
            db
        )

        return ForestUserDatabase(
            access, persistentData
        )
    }
}