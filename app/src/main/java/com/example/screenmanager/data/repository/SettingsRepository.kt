package com.example.screenmanager.data.repository

import com.example.screenmanager.domain.rules.RulesSnapshot
import com.example.screenmanager.model.AppLimitRule
import com.example.screenmanager.model.EmergencySessionConfig
import com.example.screenmanager.model.ScheduleRule
import com.example.screenmanager.model.SessionLimitRule
import com.example.screenmanager.model.ShortVideoConfig
import com.example.screenmanager.model.WakeUpConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Fasada nad svim korisničkim pravilima.
 *
 * UI piše kroz ovu klasu, a monitoring servis i accessibility servis čitaju
 * [observeRulesSnapshot] — ista pravila koja korisnik podesi su ona koja se
 * izvršavaju (ranije servisi uopšte nisu čitali ova pravila).
 */
class SettingsRepository(
    private val appLimitRulesRepository: AppLimitRulesRepository,
    private val shortVideoConfigRepository: ShortVideoConfigRepository,
    private val scheduleRulesRepository: ScheduleRulesRepository,
    private val wakeUpConfigRepository: WakeUpConfigRepository,
    private val emergencySessionRepository: EmergencySessionConfigRepository,
    private val sessionLimitRulesRepository: SessionLimitRulesRepository
) {
    /** Svi izvori pravila spojeni u jedan snimak za [com.example.screenmanager.domain.rules.RulesEngine]. */
    fun observeRulesSnapshot(): Flow<RulesSnapshot> {
        val limits = combine(
            appLimitRulesRepository.observeAll(),
            scheduleRulesRepository.observeAll(),
            sessionLimitRulesRepository.observeAll()
        ) { appLimits, schedules, sessions -> Triple(appLimits, schedules, sessions) }

        val configs = combine(
            shortVideoConfigRepository.observe(),
            wakeUpConfigRepository.observe(),
            emergencySessionRepository.observe()
        ) { shorts, wakeUp, emergency -> Triple(shorts, wakeUp, emergency) }

        return combine(limits, configs) { (appLimits, schedules, sessions), (shorts, wakeUp, emergency) ->
            RulesSnapshot(
                appLimits = appLimits,
                schedules = schedules,
                sessionLimits = sessions,
                shortVideo = shorts,
                wakeUp = wakeUp,
                emergency = emergency
            )
        }.distinctUntilChanged()
    }

    // App Limit Rules
    fun observeAppLimitRules(): Flow<List<AppLimitRule>> = appLimitRulesRepository.observeAll()

    fun observeEnabledAppLimitRules(): Flow<List<AppLimitRule>> = appLimitRulesRepository.observeEnabled()

    suspend fun addAppLimitRule(rule: AppLimitRule) = appLimitRulesRepository.add(rule)

    suspend fun updateAppLimitRule(rule: AppLimitRule) = appLimitRulesRepository.update(rule)

    suspend fun deleteAppLimitRule(rule: AppLimitRule) = appLimitRulesRepository.delete(rule)

    suspend fun deleteAppLimitRuleById(id: String) = appLimitRulesRepository.deleteById(id)

    // Session (interval) limit rules
    fun observeSessionLimitRules(): Flow<List<SessionLimitRule>> = sessionLimitRulesRepository.observeAll()

    suspend fun upsertSessionLimitRule(rule: SessionLimitRule) = sessionLimitRulesRepository.upsert(rule)

    suspend fun deleteSessionLimitRuleById(id: String) = sessionLimitRulesRepository.deleteById(id)

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
