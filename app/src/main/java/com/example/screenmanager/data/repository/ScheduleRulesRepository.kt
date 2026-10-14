package com.example.screenmanager.data.repository

import com.example.screenmanager.data.local.ScheduleRuleDao
import com.example.screenmanager.data.local.toEntity
import com.example.screenmanager.data.local.toModel
import com.example.screenmanager.model.ScheduleRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScheduleRulesRepository(private val dao: ScheduleRuleDao) {
    fun observeAll(): Flow<List<ScheduleRule>> =
        dao.observeAll().map { entities -> entities.map { it.toModel() } }

    suspend fun update(rule: ScheduleRule) = dao.upsert(rule.toEntity())

    suspend fun deleteById(id: String) = dao.deleteById(id)
}
