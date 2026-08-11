package com.example.screenmanager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.screenmanager.data.repository.SettingsRepository
import com.example.screenmanager.model.AppLimitRule
import com.example.screenmanager.model.AppOption
import com.example.screenmanager.model.EmergencySessionConfig
import com.example.screenmanager.model.ScheduleRule
import com.example.screenmanager.model.ShortVideoConfig
import com.example.screenmanager.model.WakeUpConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

/**
 * ViewModel za settings i pravila blokiranja.
 *
 * Ovaj sloj povezuje UI sa repozitorijumima i upravlja izmenama konfiguracija
 * za wake-up, shorts, schedules, app limits i emergency session.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val wakeUpConfig: StateFlow<WakeUpConfig?> = 
        settingsRepository.observeWakeUpConfig().stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, WakeUpConfig())

    val shortVideoConfig: StateFlow<ShortVideoConfig?> = 
        settingsRepository.observeShortVideoConfig().stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, ShortVideoConfig())

    val scheduleRules: StateFlow<List<ScheduleRule>> = 
        settingsRepository.observeScheduleRules().stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, emptyList())

    val appLimitRules: StateFlow<List<AppLimitRule>> = 
        settingsRepository.observeAppLimitRules().stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, emptyList())

    val emergencySession: StateFlow<EmergencySessionConfig?> = 
        settingsRepository.observeEmergencySessionConfig().stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, EmergencySessionConfig())

    private val _availableApps = MutableStateFlow(defaultAppOptions())
    val availableApps: StateFlow<List<AppOption>> = _availableApps.asStateFlow()

    /**
     * Upisuje novu konfiguraciju wake-up pravila.
     */
    fun updateWakeUpConfig(config: WakeUpConfig) {
        viewModelScope.launch {
            settingsRepository.updateWakeUpConfig(config)
        }
    }

    /**
     * Upisuje novu konfiguraciju shorts/reels ograničenja.
     */
    fun updateShortVideoConfig(config: ShortVideoConfig) {
        viewModelScope.launch {
            settingsRepository.updateShortVideoConfig(config)
        }
    }

    /**
     * Menja emergency session konfiguraciju.
     */
    fun updateEmergencySession(config: EmergencySessionConfig) {
        viewModelScope.launch {
            settingsRepository.updateEmergencySessionConfig(config)
        }
    }

    /**
     * Uključuje ili isključuje postojeće schedule pravilo.
     */
    fun toggleScheduleRule(ruleId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            val rules = scheduleRules.value
            val rule = rules.find { it.id == ruleId }
            if (rule != null) {
                settingsRepository.updateScheduleRule(rule.copy(isEnabled = isEnabled))
            }
        }
    }

    /**
     * Dodaje novo podrazumevano schedule pravilo.
     */
    fun addScheduleRule() {
        viewModelScope.launch {
            val newRule = ScheduleRule(
                id = UUID.randomUUID().toString(),
                name = "New schedule",
                startTime = LocalTime.of(22, 0),
                endTime = LocalTime.of(7, 0),
                daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY),
                selectedAppIds = emptyList(),
                isEnabled = true
            )
            settingsRepository.addScheduleRule(newRule)
        }
    }

    /**
     * Briše schedule pravilo po identifikatoru.
     */
    fun removeScheduleRule(ruleId: String) {
        viewModelScope.launch {
            settingsRepository.deleteScheduleRuleById(ruleId)
        }
    }

    /**
     * Uključuje ili isključuje app limit pravilo.
     */
    fun toggleAppLimitRule(ruleId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            val rules = appLimitRules.value
            val rule = rules.find { it.id == ruleId }
            if (rule != null) {
                settingsRepository.updateAppLimitRule(rule.copy(isEnabled = isEnabled))
            }
        }
    }

    /**
     * Čuva izmenjeno app limit pravilo.
     */
    fun updateAppLimitRule(rule: AppLimitRule) {
        viewModelScope.launch {
            settingsRepository.updateAppLimitRule(rule)
        }
    }

    /**
     * Dodaje primer app limit pravila sa podrazumevanim vrednostima.
     */
    fun addAppLimitRule() {
        viewModelScope.launch {
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
    }

    /**
     * Briše app limit pravilo po identifikatoru.
     */
    fun removeAppLimitRule(ruleId: String) {
        viewModelScope.launch {
            settingsRepository.deleteAppLimitRuleById(ruleId)
        }
    }

    /**
     * Menja lokalnu listu dostupnih aplikacija koje dijalozi prikazuju.
     */
    fun replaceAvailableApps(apps: List<AppOption>) {
        _availableApps.value = apps
    }

    /**
     * Vraća statičnu listu aplikacija ako UI još nema realne podatke.
     */
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
