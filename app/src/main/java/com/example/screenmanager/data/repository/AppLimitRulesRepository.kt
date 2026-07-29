package com.example.screenmanager.data.repository

import com.example.screenmanager.data.local.AppLimitRuleDao
import com.example.screenmanager.data.local.toEntity
import com.example.screenmanager.data.local.toModel
import com.example.screenmanager.model.AppLimitRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppLimitRulesRepository(private val dao: AppLimitRuleDao) {
    fun observeAll(): Flow<List<AppLimitRule>> =
        dao.observeAll().map { entities -> entities.map { it.toModel() } }

    fun observeEnabled(): Flow<List<AppLimitRule>> =
        dao.observeEnabled().map { entities -> entities.map { it.toModel() } }

    suspend fun add(rule: AppLimitRule) = dao.upsert(rule.toEntity())

    suspend fun update(rule: AppLimitRule) = dao.upsert(rule.toEntity())

    suspend fun delete(rule: AppLimitRule) = dao.delete(rule.toEntity())

    suspend fun deleteById(id: String) = dao.deleteById(id)

    suspend fun getById(id: String): AppLimitRule? = dao.findById(id)?.toModel()
}
