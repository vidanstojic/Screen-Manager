package com.example.screenmanager.data.repository

import com.example.screenmanager.data.local.WakeUpConfigDao
import com.example.screenmanager.data.local.toEntity
import com.example.screenmanager.data.local.toModel
import com.example.screenmanager.model.WakeUpConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WakeUpConfigRepository(private val dao: WakeUpConfigDao) {
    fun observe(): Flow<WakeUpConfig?> =
        dao.observe().map { entity -> entity?.toModel() }

    suspend fun update(config: WakeUpConfig) = dao.upsert(config.toEntity())

    suspend fun get(): WakeUpConfig? = dao.get()?.toModel()
}
