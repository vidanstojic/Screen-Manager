package com.example.screenmanager.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppInternalStateDao {
    @Upsert
    suspend fun upsert(state: AppInternalState)

    @Query("SELECT stateValueLong FROM app_internal_state WHERE stateKey = :key LIMIT 1")
    suspend fun getLong(key: String): Long?

    @Query("SELECT stateValueLong FROM app_internal_state WHERE stateKey = :key LIMIT 1")
    fun observeLong(key: String): Flow<Long?>

    @Query("SELECT * FROM app_internal_state WHERE stateKey LIKE :prefix || '%'")
    suspend fun getWithPrefix(prefix: String): List<AppInternalState>

    @Query("SELECT * FROM app_internal_state WHERE stateKey LIKE :prefix || '%'")
    fun observeWithPrefix(prefix: String): Flow<List<AppInternalState>>

    @Query("DELETE FROM app_internal_state WHERE stateKey = :key")
    suspend fun delete(key: String)
}
