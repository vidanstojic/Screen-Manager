package com.example.screenmanager.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockConfigDao {
    @Upsert
    suspend fun upsert(config: BlockConfig)

    @Upsert
    suspend fun upsertAll(configs: List<BlockConfig>)

    @Query("SELECT * FROM block_config ORDER BY packageName")
    fun observeAll(): Flow<List<BlockConfig>>

    @Query("SELECT * FROM block_config WHERE packageName = :packageName LIMIT 1")
    suspend fun find(packageName: String): BlockConfig?

    @Query("SELECT * FROM block_config WHERE isBlockedDuringWakeup = 1")
    suspend fun wakeupBlockedPackages(): List<BlockConfig>
}
