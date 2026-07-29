package com.example.screenmanager.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortVideoConfigDao {
    @Upsert
    suspend fun upsert(config: ShortVideoConfigEntity)

    @Query("SELECT * FROM short_video_configs WHERE id = 'shorts_global' LIMIT 1")
    fun observe(): Flow<ShortVideoConfigEntity?>

    @Query("SELECT * FROM short_video_configs WHERE id = 'shorts_global' LIMIT 1")
    suspend fun get(): ShortVideoConfigEntity?
}
