package com.example.screenmanager.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleRuleDao {
    @Upsert
    suspend fun upsert(rule: ScheduleRuleEntity)

    @Query("SELECT * FROM schedule_rules ORDER BY name")
    fun observeAll(): Flow<List<ScheduleRuleEntity>>

    @Query("DELETE FROM schedule_rules WHERE id = :id")
    suspend fun deleteById(id: String)
}
