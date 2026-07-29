package com.example.screenmanager.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencySessionConfigDao {
    @Upsert
    suspend fun upsert(config: EmergencySessionConfigEntity)

    @Query("SELECT * FROM emergency_sessions WHERE id = 'emergency_global' LIMIT 1")
    fun observe(): Flow<EmergencySessionConfigEntity?>

    @Query("SELECT * FROM emergency_sessions WHERE id = 'emergency_global' LIMIT 1")
    suspend fun get(): EmergencySessionConfigEntity?
}
