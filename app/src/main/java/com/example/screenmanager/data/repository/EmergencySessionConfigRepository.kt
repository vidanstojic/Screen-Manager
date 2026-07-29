package com.example.screenmanager.data.repository

import com.example.screenmanager.data.local.EmergencySessionConfigDao
import com.example.screenmanager.data.local.toEntity
import com.example.screenmanager.data.local.toModel
import com.example.screenmanager.model.EmergencySessionConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EmergencySessionConfigRepository(private val dao: EmergencySessionConfigDao) {
    fun observe(): Flow<EmergencySessionConfig?> =
        dao.observe().map { entity -> entity?.toModel() }

    suspend fun update(config: EmergencySessionConfig) = dao.upsert(config.toEntity())

    suspend fun get(): EmergencySessionConfig? = dao.get()?.toModel()
}
