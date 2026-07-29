package com.example.screenmanager.data.repository

import com.example.screenmanager.model.AppLimitRule
import com.example.screenmanager.model.EmergencySessionConfig
import com.example.screenmanager.model.ScheduleRule
import com.example.screenmanager.model.ShortVideoConfig
import com.example.screenmanager.model.WakeUpConfig
import kotlinx.coroutines.flow.Flow

class SettingsRepository(
    private val appLimitRulesRepository: AppLimitRulesRepository,
    private val shortVideoConfigRepository: ShortVideoConfigRepository,
    private val scheduleRulesRepository: ScheduleRulesRepository,
    private val wakeUpConfigRepository: WakeUpConfigRepository,
    private val emergencySessionRepository: EmergencySessionConfigRepository
) {
    // App Limit Rules
    fun observeAppLimitRules(): Flow<List<AppLimitRule>> = appLimitRulesRepository.observeAll()

    fun observeEnabledAppLimitRules(): Flow<List<AppLimitRule>> = appLimitRulesRepository.observeEnabled()

    suspend fun addAppLimitRule(rule: AppLimitRule) = appLimitRulesRepository.add(rule)

    suspend fun updateAppLimitRule(rule: AppLimitRule) = appLimitRulesRepository.update(rule)

    suspend fun deleteAppLimitRule(rule: AppLimitRule) = appLimitRulesRepository.delete(rule)

    suspend fun deleteAppLimitRuleById(id: String) = appLimitRulesRepository.deleteById(id)

    // Short Video Config
    fun observeShortVideoConfig(): Flow<ShortVideoConfig?> = shortVideoConfigRepository.observe()

    suspend fun updateShortVideoConfig(config: ShortVideoConfig) = shortVideoConfigRepository.update(config)

    suspend fun getShortVideoConfig(): ShortVideoConfig? = shortVideoConfigRepository.get()

    // Schedule Rules
    fun observeScheduleRules(): Flow<List<ScheduleRule>> = scheduleRulesRepository.observeAll()

    fun observeEnabledScheduleRules(): Flow<List<ScheduleRule>> = scheduleRulesRepository.observeEnabled()

    suspend fun addScheduleRule(rule: ScheduleRule) = scheduleRulesRepository.add(rule)

    suspend fun updateScheduleRule(rule: ScheduleRule) = scheduleRulesRepository.update(rule)

    suspend fun deleteScheduleRule(rule: ScheduleRule) = scheduleRulesRepository.delete(rule)

    suspend fun deleteScheduleRuleById(id: String) = scheduleRulesRepository.deleteById(id)

    // Wake Up Config
    fun observeWakeUpConfig(): Flow<WakeUpConfig?> = wakeUpConfigRepository.observe()

    suspend fun updateWakeUpConfig(config: WakeUpConfig) = wakeUpConfigRepository.update(config)

    suspend fun getWakeUpConfig(): WakeUpConfig? = wakeUpConfigRepository.get()

    // Emergency Session Config
    fun observeEmergencySessionConfig(): Flow<EmergencySessionConfig?> = emergencySessionRepository.observe()

    suspend fun updateEmergencySessionConfig(config: EmergencySessionConfig) =
        emergencySessionRepository.update(config)

    suspend fun getEmergencySessionConfig(): EmergencySessionConfig? = emergencySessionRepository.get()
}
