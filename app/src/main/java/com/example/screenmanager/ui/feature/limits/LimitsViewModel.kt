package com.example.screenmanager.ui.feature.limits

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.screenmanager.domain.ServiceLocator
import com.example.screenmanager.model.AppLimitRule
import com.example.screenmanager.model.AppOption
import com.example.screenmanager.model.EmergencySessionConfig
import com.example.screenmanager.model.ScheduleRule
import com.example.screenmanager.model.SessionLimitRule
import com.example.screenmanager.model.ShortVideoConfig
import com.example.screenmanager.model.WakeUpConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Sva korisnička pravila + lista aplikacija koje mogu da se izaberu. */
data class LimitsUiState(
    val appLimits: List<AppLimitRule>,
    val sessionLimits: List<SessionLimitRule>,
    val schedules: List<ScheduleRule>,
    val shorts: ShortVideoConfig,
    val morningLock: WakeUpConfig,
    val emergency: EmergencySessionConfig,
    val installedApps: List<AppOption>
)

/**
 * ViewModel za sva pravila blokiranja: Limits tab i forme za izmenu pravila.
 *
 * Ekrani čitaju [uiState] i zovu `save*` / `delete*`; upis u bazu odmah
 * vide i servisi koji sprovode pravila (isti repozitorijum).
 */
class LimitsViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = ServiceLocator.settingsRepository(application)
    private val installedAppsSource = ServiceLocator.installedApps(application)

    private val installedApps = MutableStateFlow<List<AppOption>>(emptyList())

    /** `null` dok se pravila prvi put ne učitaju iz baze (forme čekaju na to). */
    val uiState: StateFlow<LimitsUiState?> =
        combine(settings.observeRulesSnapshot(), installedApps) { rules, apps ->
            LimitsUiState(
                appLimits = rules.appLimits,
                sessionLimits = rules.sessionLimits,
                schedules = rules.schedules,
                shorts = rules.shortVideo ?: ShortVideoConfig(),
                morningLock = rules.wakeUp ?: WakeUpConfig(),
                emergency = rules.emergency ?: EmergencySessionConfig(),
                installedApps = apps
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { installedApps.value = installedAppsSource.launchableApps() }
    }

    /** Ime aplikacije za prikaz (keširano). */
    fun appLabel(packageName: String): String = installedAppsSource.label(packageName)

    // --- Dnevni limiti ---

    fun saveAppLimit(rule: AppLimitRule) {
        viewModelScope.launch { settings.updateAppLimitRule(rule) }
    }

    fun deleteAppLimit(ruleId: String) {
        viewModelScope.launch { settings.deleteAppLimitRuleById(ruleId) }
    }

    // --- Interval (session) pravila ---

    fun saveSessionLimit(rule: SessionLimitRule) {
        viewModelScope.launch { settings.upsertSessionLimitRule(rule) }
    }

    fun deleteSessionLimit(ruleId: String) {
        viewModelScope.launch { settings.deleteSessionLimitRuleById(ruleId) }
    }

    // --- Zakazane blokade ---

    fun saveSchedule(rule: ScheduleRule) {
        viewModelScope.launch { settings.updateScheduleRule(rule) }
    }

    fun deleteSchedule(ruleId: String) {
        viewModelScope.launch { settings.deleteScheduleRuleById(ruleId) }
    }

    // --- Globalna pravila ---

    fun saveShorts(config: ShortVideoConfig) {
        viewModelScope.launch { settings.updateShortVideoConfig(config) }
    }

    fun saveMorningLock(config: WakeUpConfig) {
        viewModelScope.launch { settings.updateWakeUpConfig(config) }
    }

    // --- Emergency pauza ---

    fun setEmergencyDuration(minutes: Int) {
        viewModelScope.launch {
            val config = settings.getEmergencySessionConfig() ?: EmergencySessionConfig()
            settings.updateEmergencySessionConfig(config.copy(defaultDurationMinutes = minutes))
        }
    }

    /** Pokreće pauzu od SADA; čuva se apsolutni trenutak isteka (radi i preko ponoći). */
    fun startEmergencyPause() {
        viewModelScope.launch {
            val config = settings.getEmergencySessionConfig() ?: EmergencySessionConfig()
            settings.updateEmergencySessionConfig(config.activated(System.currentTimeMillis()))
        }
    }

    fun endEmergencyPause() {
        viewModelScope.launch {
            val config = settings.getEmergencySessionConfig() ?: EmergencySessionConfig()
            settings.updateEmergencySessionConfig(config.ended())
        }
    }
}
