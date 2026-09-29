package com.example.screenmanager.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionLimitDao {
    @Upsert
    suspend fun upsertRule(rule: SessionLimitRuleEntity)

    @Query("DELETE FROM session_limit_rules WHERE id = :id")
    suspend fun deleteRuleById(id: String)

    @Query("SELECT * FROM session_limit_rules ORDER BY name")
    fun observeRules(): Flow<List<SessionLimitRuleEntity>>

    @Query("SELECT COUNT(*) FROM session_limit_rules")
    suspend fun countRules(): Int

    @Upsert
    suspend fun upsertStates(states: List<SessionLimitStateEntity>)

    @Query("SELECT * FROM session_limit_state WHERE ruleId = :ruleId LIMIT 1")
    suspend fun state(ruleId: String): SessionLimitStateEntity?

    @Query("SELECT * FROM session_limit_state")
    suspend fun allStates(): List<SessionLimitStateEntity>

    @Query("SELECT * FROM session_limit_state")
    fun observeStates(): Flow<List<SessionLimitStateEntity>>

    /** Shorts/Reels interval stanja (`shorts:<paket>`) nemaju red u session_limit_rules — ne brišu se. */
    @Query("DELETE FROM session_limit_state WHERE ruleId NOT IN (SELECT id FROM session_limit_rules) AND ruleId NOT LIKE 'shorts:%'")
    suspend fun deleteOrphanStates()
}
