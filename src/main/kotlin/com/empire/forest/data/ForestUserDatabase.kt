package com.empire.forest.data

import com.empire.ignite.storage.CacheAndDatabaseResourceAccess
import com.empire.ignite.storage.IgniteSQLStorage
import com.empire.ignite.storage.KeyedResourceAccess
import com.empire.ignite.storage.MapResourceCache
import com.empire.ignite.storage.SQLExecutionHelper
import com.empire.ignite.storage.SQLiteConfig
import com.empire.ignite.storage.SQLiteStorage
import com.empire.ignite.util.UnloadableResource
import java.sql.DatabaseMetaData
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Statement
import java.sql.Types
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future


data class ForestUserData(
    var coins: Int, var lastSelectedHunterKit: String? = null, var lastSelectedSurvivorKit: String? = null
)

class ForestUserDatabase(
    val access: CacheAndDatabaseResourceAccess<UUID, ForestUserData>,
    private val storage: IgniteSQLStorage<*>
) : UnloadableResource {
    companion object {
        fun getPlayerDataBatchQuery(size: Int) : String {
            val placeholders = List(size) { "?" }.joinToString(", ")
            return """
                SELECT player, coins, hunter_sel_class, survivor_sel_class FROM forest_user_data 
                WHERE player in ($placeholders)
            """.trimIndent()
        }

        private val UPDATE_USER_DATA_QUERY = """
            UPDATE forest_user_data SET coins = ?, hunter_sel_class = ?, survivor_sel_class = ? WHERE player = ?
        """.trimIndent()
    }

    private val executor = Executors.newVirtualThreadPerTaskExecutor()

    fun getDataBatch(ids: List<UUID>) : Future<Map<UUID, ForestUserData>> {
        return executor.submit<Map<UUID, ForestUserData>> {
            storage.sql().executeQueryStatement(
                getPlayerDataBatchQuery(ids.size),
                SQLExecutionHelper.StatementExecutionCallback.QueryStatement(
                    configure = { stmt ->
                        var index = 1
                        for (id in ids) {
                            stmt.setString(index++, id.toString())
                        }
                    },
                    handler = { rs ->
                        val data : MutableMap<UUID, ForestUserData> = mutableMapOf()
                        while (rs.next()) {
                            val user = rs.getString("player")
                            val coins = rs.getInt("coins")
                            val lastHunter = rs.getString("hunter_sel_class")
                            val lastSurvivor = rs.getString("survivor_sel_class")
                            data +=
                                UUID.fromString(user) to
                                ForestUserData(
                                    coins, lastHunter, lastSurvivor
                                )
                        }
                        data
                    }
                )
            )
        }
    }

    fun setDataBatch(data: List<Pair<UUID, ForestUserData>>) : Future<Boolean> {
        return executor.submit<Boolean> {
            try {
                storage.connection.let { conn ->
                    conn.setAutoCommit(false)
                    try {
                        conn.prepareStatement(UPDATE_USER_DATA_QUERY).use { pstmt ->
                            for ((id, record) in data) {
                                pstmt.setInt(1, record.coins)
                                pstmt.setString(2, record.lastSelectedHunterKit)
                                pstmt.setString(3, record.lastSelectedSurvivorKit)
                                pstmt.setString(4, id.toString())
                                pstmt.addBatch()
                            }
                            pstmt.executeBatch()
                            conn.commit()
                        }
                    } catch (e: SQLException) {
                        conn.rollback()
                        throw e
                    } finally {
                        conn.setAutoCommit(true)
                    }
                }
                true
            } catch (e: SQLException) {
                e.printStackTrace()
                false
            }
        }
    }

    override fun unload(external: Boolean) {
        access.flushAll()
        storage.unload()
    }
}

object ForestUserDatabaseProvider {
    private val USER_DATA_TABLE_NAME = "forest_user_data"
    private val TABLE_DEFINITION =
        """
            CREATE TABLE IF NOT EXISTS forest_user_data (
                player VARCHAR(36) NOT NULL PRIMARY KEY, coins INTEGER NOT NULL
            )
        """.trimIndent()

    private val GET_PLAYER_DATA_STATEMENT =
        """
            SELECT coins, hunter_sel_class, survivor_sel_class FROM forest_user_data WHERE player = ?
        """.trimIndent()

    private val INSERT_PLAYER_DATA_STATEMENT =
        """
            INSERT INTO forest_user_data (player, coins) 
            VALUES (?, ?)
        """.trimIndent()

    private val UPDATE_PLAYER_DATA_STATEMENT =
        """
            UPDATE forest_user_data SET coins = ?, hunter_sel_class = ?, 
            survivor_sel_class = ? WHERE player = ?
        """.trimIndent()

    private val HUNTER_KIT_COLUMN_NAME = "hunter_sel_class"
    private val CREATE_LAST_HUNTER_KIT_COLUMN_QUERY =
        """
            ALTER TABLE forest_user_data ADD COLUMN hunter_sel_class TEXT DEFAULT NULL;
        """.trimIndent()

    private val SURVIVOR_KIT_COLUMN_NAME = "survivor_sel_class"
    private val CREATE_LAST_SURVIVOR_KIT_COLUMN_QUERY =
        """
            ALTER TABLE forest_user_data ADD COLUMN survivor_sel_class TEXT DEFAULT NULL;
        """.trimIndent()

    fun create(path: String) : ForestUserDatabase {
        val persistentData = SQLiteStorage()
        persistentData.initialize(SQLiteConfig(path))
        persistentData.connection.createStatement().execute(TABLE_DEFINITION)
        addColumns(persistentData)
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
                                    val coins = rs.getInt("coins")
                                    val lastHunter = rs.getString("hunter_sel_class")
                                    val lastSurvivor = rs.getString("survivor_sel_class")
                                    ForestUserData(
                                        coins, lastHunter, lastSurvivor
                                    )
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
                            nullableSetString(stmt, 2, data.lastSelectedHunterKit)
                            nullableSetString(stmt, 3, data.lastSelectedSurvivorKit)
                            stmt.setString(4, player.toString())
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

    private fun addColumns(sql: SQLiteStorage) {
        val databaseMetadata = sql.connection.metaData
        val statement = sql.connection.createStatement()
        createColumnIfNotExists(
            databaseMetadata, statement,
            USER_DATA_TABLE_NAME,
            HUNTER_KIT_COLUMN_NAME, CREATE_LAST_HUNTER_KIT_COLUMN_QUERY
        )
        createColumnIfNotExists(
            databaseMetadata, statement,
            USER_DATA_TABLE_NAME,
            SURVIVOR_KIT_COLUMN_NAME, CREATE_LAST_SURVIVOR_KIT_COLUMN_QUERY
        )
    }

    private fun createColumnIfNotExists(
        databaseMetadata: DatabaseMetaData,
        statement: Statement,
        tableName: String?, columnName: String?, alterQuery: String?
    ) {
        if (!columnExists(databaseMetadata, tableName, columnName)) {
            try {
                statement.execute(alterQuery)
            } catch (e: SQLException) {
                throw java.lang.RuntimeException(e)
            }
        }
    }


    private fun columnExists(
        databaseMetadata: DatabaseMetaData,
        tableName: String?,
        columnName: String?
    ): Boolean {
        try {
            databaseMetadata.getColumns(null, null, tableName, columnName).use { resultSet ->
                return resultSet.next()
            }
        } catch (e: SQLException) {
            throw RuntimeException(e)
        }
    }

    private fun nullableSetString(stmt: PreparedStatement, index: Int, v: String?) {
        if (v != null) {
            stmt.setString(index, v)
        } else {
            stmt.setNull(index, Types.VARCHAR)
        }
    }
}