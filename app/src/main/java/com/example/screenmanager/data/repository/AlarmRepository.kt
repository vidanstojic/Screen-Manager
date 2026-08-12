package com.example.screenmanager.data.repository

import com.example.screenmanager.data.local.AlarmDao
import com.example.screenmanager.data.local.toEntity
import com.example.screenmanager.data.local.toModel
import com.example.screenmanager.model.AlarmRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlarmRepository(
    private val dao: AlarmDao
) {
    fun observeAll(): Flow<List<AlarmRule>> =
        dao.observeAll().map { entities -> entities.map { it.toModel() } }

    suspend fun upsert(alarm: AlarmRule) = dao.upsert(alarm.toEntity())

    suspend fun findById(id: Long): AlarmRule? = dao.findById(id)?.toModel()

    suspend fun getEnabled(): List<AlarmRule> = dao.getEnabled().map { it.toModel() }

    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
