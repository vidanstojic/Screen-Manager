package com.example.screenmanager.data.repository

import com.example.screenmanager.data.local.ShortVideoConfigDao
import com.example.screenmanager.data.local.toEntity
import com.example.screenmanager.data.local.toModel
import com.example.screenmanager.model.ShortVideoConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShortVideoConfigRepository(private val dao: ShortVideoConfigDao) {
    fun observe(): Flow<ShortVideoConfig?> =
        dao.observe().map { entity -> entity?.toModel() }

    suspend fun update(config: ShortVideoConfig) = dao.upsert(config.toEntity())

    suspend fun get(): ShortVideoConfig? = dao.get()?.toModel()
}
