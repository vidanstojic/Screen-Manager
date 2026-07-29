package com.example.screenmanager.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WakeUpConfigDao {
    @Upsert
    suspend fun upsert(config: WakeUpConfigEntity)

    @Query("SELECT * FROM wake_up_configs WHERE id = 'wakeup_global' LIMIT 1")
    fun observe(): Flow<WakeUpConfigEntity?>

    @Query("SELECT * FROM wake_up_configs WHERE id = 'wakeup_global' LIMIT 1")
    suspend fun get(): WakeUpConfigEntity?
}
