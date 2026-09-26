package com.example.screenmanager.data.repository

import com.example.screenmanager.data.local.SessionLimitDao
import com.example.screenmanager.data.local.toEntity
import com.example.screenmanager.data.local.toModel
import com.example.screenmanager.model.SessionLimitRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SessionLimitRulesRepository(private val dao: SessionLimitDao) {
    fun observeAll(): Flow<List<SessionLimitRule>> =
        dao.observeRules().map { entities -> entities.map { it.toModel() } }

    suspend fun upsert(rule: SessionLimitRule) = dao.upsertRule(rule.toEntity())

    suspend fun deleteById(id: String) {
        dao.deleteRuleById(id)
        dao.deleteOrphanStates()
    }
}
