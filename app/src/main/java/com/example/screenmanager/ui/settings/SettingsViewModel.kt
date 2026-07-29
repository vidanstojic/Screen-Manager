package com.example.screenmanager.ui.settings

import androidx.lifecycle.ViewModel
import com.example.screenmanager.model.AppLimitRule
import com.example.screenmanager.model.AppOption
import com.example.screenmanager.model.EmergencySessionConfig
import com.example.screenmanager.model.ScheduleRule
import com.example.screenmanager.model.ShortVideoConfig
import com.example.screenmanager.model.WakeUpConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

class SettingsViewModel : ViewModel() {
    private val _wakeUpConfig = MutableStateFlow(WakeUpConfig())
    val wakeUpConfig: StateFlow<WakeUpConfig> = _wakeUpConfig.asStateFlow()

    private val _shortVideoConfig = MutableStateFlow(ShortVideoConfig())
    val shortVideoConfig: StateFlow<ShortVideoConfig> = _shortVideoConfig.asStateFlow()

    private val _scheduleRules = MutableStateFlow(initialScheduleRules())
    val scheduleRules: StateFlow<List<ScheduleRule>> = _scheduleRules.asStateFlow()

    private val _appLimitRules = MutableStateFlow(initialAppLimitRules())
    val appLimitRules: StateFlow<List<AppLimitRule>> = _appLimitRules.asStateFlow()

    private val _emergencySession = MutableStateFlow(EmergencySessionConfig())
    val emergencySession: StateFlow<EmergencySessionConfig> = _emergencySession.asStateFlow()

    private val _availableApps = MutableStateFlow(defaultAppOptions())
    val availableApps: StateFlow<List<AppOption>> = _availableApps.asStateFlow()

    fun updateWakeUpConfig(config: WakeUpConfig) {
        _wakeUpConfig.value = config
    }

    fun updateShortVideoConfig(config: ShortVideoConfig) {
        _shortVideoConfig.value = config
    }

    fun updateEmergencySession(config: EmergencySessionConfig) {
        _emergencySession.value = config
    }

    fun toggleScheduleRule(ruleId: String, isEnabled: Boolean) {
        _scheduleRules.update { rules ->
            rules.map { if (it.id == ruleId) it.copy(isEnabled = isEnabled) else it }
        }
    }

    fun addScheduleRule() {
        val newRule = ScheduleRule(
            id = UUID.randomUUID().toString(),
            name = "New schedule",
            startTime = LocalTime.of(22, 0),
            endTime = LocalTime.of(7, 0),
            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY),
            selectedAppIds = emptyList(),
            isEnabled = true
        )
        _scheduleRules.update { it + newRule }
    }

    fun removeScheduleRule(ruleId: String) {
        _scheduleRules.update { rules -> rules.filterNot { it.id == ruleId } }
    }

    fun toggleAppLimitRule(ruleId: String, isEnabled: Boolean) {
        _appLimitRules.update { rules ->
            rules.map { if (it.id == ruleId) it.copy(isEnabled = isEnabled) else it }
        }
    }

    fun updateAppLimitRule(rule: AppLimitRule) {
        _appLimitRules.update { rules ->
            val next = rules.toMutableList()
            val index = next.indexOfFirst { it.id == rule.id }
            if (index >= 0) {
                next[index] = rule
            } else {
                next.add(rule)
            }
            next
        }
    }

    fun addAppLimitRule() {
        updateAppLimitRule(
            AppLimitRule(
                id = UUID.randomUUID().toString(),
                name = "Daily limit",
                selectedAppIds = listOf("com.google.android.youtube"),
                dailyLimitMinutes = 60,
                blockDurationMinutes = 60,
                description = "Blocks the app when the limit expires."
            )
        )
    }

    fun removeAppLimitRule(ruleId: String) {
        _appLimitRules.update { rules -> rules.filterNot { it.id == ruleId } }
    }

    fun replaceAvailableApps(apps: List<AppOption>) {
        _availableApps.value = apps
    }

    private fun initialScheduleRules(): List<ScheduleRule> {
        return listOf(
            ScheduleRule(
                id = UUID.randomUUID().toString(),
                name = "Night focus",
                startTime = LocalTime.of(22, 0),
                endTime = LocalTime.of(7, 0),
                daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY),
                selectedAppIds = listOf("com.google.android.youtube", "com.instagram.android"),
                isEnabled = true
            ),
            ScheduleRule(
                id = UUID.randomUUID().toString(),
                name = "Work hours",
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(17, 0),
                daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                selectedAppIds = listOf("com.google.android.youtube"),
                isEnabled = false
            )
        )
    }

    private fun initialAppLimitRules(): List<AppLimitRule> {
        return listOf(
            AppLimitRule(
                id = UUID.randomUUID().toString(),
                name = "YouTube daily cap",
                selectedAppIds = listOf("com.google.android.youtube"),
                dailyLimitMinutes = 45,
                blockDurationMinutes = 45,
                description = "Blocks YouTube for the same period after the limit expires."
            ),
            AppLimitRule(
                id = UUID.randomUUID().toString(),
                name = "Instagram daily cap",
                selectedAppIds = listOf("com.instagram.android"),
                dailyLimitMinutes = 30,
                blockDurationMinutes = 30,
                description = "Blocks Instagram and its short-form surfaces."
            )
        )
    }

    private fun defaultAppOptions(): List<AppOption> {
        return listOf(
            AppOption("com.google.android.youtube", "YouTube", "Video", "YT"),
            AppOption("com.instagram.android", "Instagram", "Social", "IG"),
            AppOption("com.zhiliaoapp.musically", "TikTok", "Short video", "TT"),
            AppOption("com.whatsapp", "WhatsApp", "Messaging", "WA"),
            AppOption("com.android.chrome", "Chrome", "Browser", "CH"),
            AppOption("com.android.settings", "Settings", "System", "SE")
        )
    }
}
