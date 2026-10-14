package com.example.screenmanager.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLimitRuleDao {
    @Upsert
    suspend fun upsert(rule: AppLimitRuleEntity)

    @Query("SELECT * FROM app_limit_rules ORDER BY name")
    fun observeAll(): Flow<List<AppLimitRuleEntity>>

    @Query("DELETE FROM app_limit_rules WHERE id = :id")
    suspend fun deleteById(id: String)
}
