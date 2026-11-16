package com.example.screenmanager.ui.feature.limits

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.screenmanager.domain.ServiceLocator
import com.example.screenmanager.domain.rules.LimitLocks
import com.example.screenmanager.domain.rules.LimitsLock
import com.example.screenmanager.domain.rules.RuleLockKeys
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
    val installedApps: List<AppOption>,
    /** Opšte i pojedinačna zaključavanja (možda istekla — vidi [LimitLocks.lockedUntil]). */
    val locks: LimitLocks = LimitLocks()
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
    private val lockStore = ServiceLocator.limitsLockStore(application)

    private val installedApps = MutableStateFlow<List<AppOption>>(emptyList())

    /** `null` dok se pravila prvi put ne učitaju iz baze (forme čekaju na to). */
    val uiState: StateFlow<LimitsUiState?> =
        combine(settings.observeRulesSnapshot(), installedApps, lockStore.observe()) { rules, apps, locks ->
            LimitsUiState(
                appLimits = rules.appLimits,
                sessionLimits = rules.sessionLimits,
                schedules = rules.schedules,
                shorts = rules.shortVideo ?: ShortVideoConfig(),
                morningLock = rules.wakeUp ?: WakeUpConfig(),
                emergency = rules.emergency ?: EmergencySessionConfig(),
                installedApps = apps,
                locks = locks
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { installedApps.value = installedAppsSource.launchableApps() }
    }

    /** Ime aplikacije za prikaz (keširano). */
    fun appLabel(packageName: String): String = installedAppsSource.label(packageName)

    // --- Zaključavanje limita (TODO-002) ---
    // Sve izmene pravila prolaze kroz ovu proveru (i sa liste i iz formi), ne samo kroz UI.

    private fun elapsed(): Long = SystemClock.elapsedRealtime()

    /** Pravilo [ruleKey] je zaključano (samo ono ili sva pravila). */
    private fun lockedFor(ruleKey: String): Boolean =
        lockStore.current().lockedUntil(ruleKey, System.currentTimeMillis(), elapsed()) != null

    private fun anyLockActive(): Boolean = lockStore.current().anyActive(System.currentTimeMillis(), elapsed())

    /** Pravila još nisu učitana, pa se ne zna da li je izmena novo pravilo: tokom zaključavanja odbij. */
    private fun rulesNotLoadedWhileLocked(): Boolean = uiState.value == null && anyLockActive()

    /** Zaključava SVA pravila na [durationMs]; aktivno zaključavanje se samo produžava. */
    fun lockAllLimits(durationMs: Long) {
        lockStore.save(lockStore.current().withAllLocked(System.currentTimeMillis(), elapsed(), durationMs))
    }

    /** Zaključava jedno pravilo ([RuleLockKeys]) na [durationMs]. */
    fun lockRule(ruleKey: String, durationMs: Long) {
        lockStore.save(lockStore.current().withRuleLocked(ruleKey, System.currentTimeMillis(), elapsed(), durationMs))
    }

    // --- Dnevni limiti ---

    fun saveAppLimit(rule: AppLimitRule) {
        if (rulesNotLoadedWhileLocked()) return
        val existing = uiState.value?.appLimits?.firstOrNull { it.id == rule.id }
        if (!LimitsLock.allowsSave(lockedFor(RuleLockKeys.appLimit(rule.id)), existing, rule) { it.copy(isEnabled = true) }) return
        viewModelScope.launch { settings.updateAppLimitRule(rule) }
    }

    fun deleteAppLimit(ruleId: String) {
        if (lockedFor(RuleLockKeys.appLimit(ruleId))) return
        viewModelScope.launch { settings.deleteAppLimitRuleById(ruleId) }
    }

    // --- Interval (session) pravila ---

    fun saveSessionLimit(rule: SessionLimitRule) {
        if (rulesNotLoadedWhileLocked()) return
        val existing = uiState.value?.sessionLimits?.firstOrNull { it.id == rule.id }
        if (!LimitsLock.allowsSave(lockedFor(RuleLockKeys.sessionLimit(rule.id)), existing, rule) { it.copy(isEnabled = true) }) return
        viewModelScope.launch { settings.upsertSessionLimitRule(rule) }
    }

    fun deleteSessionLimit(ruleId: String) {
        if (lockedFor(RuleLockKeys.sessionLimit(ruleId))) return
        viewModelScope.launch { settings.deleteSessionLimitRuleById(ruleId) }
    }

    // --- Zakazane blokade ---

    fun saveSchedule(rule: ScheduleRule) {
        if (rulesNotLoadedWhileLocked()) return
        val existing = uiState.value?.schedules?.firstOrNull { it.id == rule.id }
        if (!LimitsLock.allowsSave(lockedFor(RuleLockKeys.schedule(rule.id)), existing, rule) { it.copy(isEnabled = true) }) return
        viewModelScope.launch { settings.updateScheduleRule(rule) }
    }

    fun deleteSchedule(ruleId: String) {
        if (lockedFor(RuleLockKeys.schedule(ruleId))) return
        viewModelScope.launch { settings.deleteScheduleRuleById(ruleId) }
    }

    // --- Globalna pravila ---

    fun saveShorts(config: ShortVideoConfig) {
        if (rulesNotLoadedWhileLocked()) return
        if (!LimitsLock.allowsSave(lockedFor(RuleLockKeys.SHORTS), uiState.value?.shorts, config) { it.copy(isEnabled = true) }) return
        viewModelScope.launch { settings.updateShortVideoConfig(config) }
    }

    fun saveMorningLock(config: WakeUpConfig) {
        if (rulesNotLoadedWhileLocked()) return
        if (!LimitsLock.allowsSave(lockedFor(RuleLockKeys.MORNING_LOCK), uiState.value?.morningLock, config) { it.copy(isEnabled = true) }) return
        viewModelScope.launch { settings.updateWakeUpConfig(config) }
    }

    // --- Emergency pauza (nije dostupna dok traje bilo koje zaključavanje — zaobišla bi ga) ---

    fun setEmergencyDuration(minutes: Int) {
        if (anyLockActive()) return
        viewModelScope.launch {
            val config = settings.getEmergencySessionConfig() ?: EmergencySessionConfig()
            settings.updateEmergencySessionConfig(config.copy(defaultDurationMinutes = minutes))
        }
    }

    /** Pokreće pauzu od SADA; čuva se apsolutni trenutak isteka (radi i preko ponoći). */
    fun startEmergencyPause() {
        if (anyLockActive()) return
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
