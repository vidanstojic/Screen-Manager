package com.example.screenmanager.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLimitRuleDao {
    @Upsert
    suspend fun upsert(rule: AppLimitRuleEntity)

    @Delete
    suspend fun delete(rule: AppLimitRuleEntity)

    @Query("SELECT * FROM app_limit_rules ORDER BY name")
    fun observeAll(): Flow<List<AppLimitRuleEntity>>

    @Query("SELECT * FROM app_limit_rules WHERE isEnabled = 1")
    fun observeEnabled(): Flow<List<AppLimitRuleEntity>>

    @Query("SELECT * FROM app_limit_rules WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): AppLimitRuleEntity?

    @Query("DELETE FROM app_limit_rules WHERE id = :id")
    suspend fun deleteById(id: String)
}
