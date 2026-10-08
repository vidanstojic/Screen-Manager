package com.example.screenmanager.ui.feature.alarms

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.screenmanager.alarm.AlarmModule
import com.example.screenmanager.alarm.data.AlarmPermission
import com.example.screenmanager.alarm.data.AlarmPermissions
import com.example.screenmanager.alarm.data.PendingSnooze
import com.example.screenmanager.alarm.domain.AlarmDetoxLock
import com.example.screenmanager.alarm.domain.PendingWakeCheck
import com.example.screenmanager.alarm.service.WakeCheckController
import com.example.screenmanager.alarm.service.AlarmRingService
import com.example.screenmanager.domain.ServiceLocator
import com.example.screenmanager.model.AlarmRule
import com.example.screenmanager.model.AppOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel za listu alarma i formu alarma. Svaka izmena se upisuje u bazu
 * i odmah (pre)zakazuje kod sistema preko AlarmScheduler-a.
 */
class AlarmsViewModel(application: Application) : AndroidViewModel(application) {
    private val alarmRepository = ServiceLocator.alarmRepository(application)
    private val alarmScheduler = ServiceLocator.alarmScheduler(application)
    private val runtimeStore = AlarmModule.runtimeStore(application)
    private val permissionChecker = AlarmPermissions(application)
    private val installedAppsSource = ServiceLocator.installedApps(application)
    private val screenManagerPermissions = ServiceLocator.permissionStateChecker(application)

    private val _installedApps = MutableStateFlow<List<AppOption>>(emptyList())

    /** Instalirane aplikacije za izbor u wake-up detox-u (učitava se jednom). */
    val installedApps: StateFlow<List<AppOption>> = _installedApps.asStateFlow()

    private val _detoxReadiness = MutableStateFlow(detoxReadiness())

    /** Da li su uključeni servisi koji sprovode wake-up detox. */
    val detoxReadiness: StateFlow<DetoxReadiness> = _detoxReadiness.asStateFlow()

    init {
        viewModelScope.launch { _installedApps.value = installedAppsSource.launchableApps() }
    }

    fun appLabel(packageName: String): String = installedAppsSource.label(packageName)

    /** `null` dok se alarmi prvi put ne učitaju iz baze. */
    val alarms: StateFlow<List<AlarmRule>?> = alarmRepository.observeAll()
        .map { list -> list.sortedWith(compareBy({ it.hour }, { it.minute })) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Aktivni (ili istekli) wake-up detox — lista prikazuje karticu dok traje. */
    val detoxLock: StateFlow<AlarmDetoxLock?> = AlarmModule.detoxStore(application).lock

    /** Zakazane wake-up check provere (po id-ju alarma). */
    val wakeChecks: StateFlow<Map<Long, PendingWakeCheck>> = runtimeStore.wakeChecks

    /** Odloženi alarmi (po id-ju) — lista prikazuje "Snoozed until". */
    val snoozes: StateFlow<Map<Long, PendingSnooze>> = runtimeStore.snoozes

    private val _permissions = MutableStateFlow(permissionChecker.snapshot())
    val permissions: StateFlow<List<AlarmPermission>> = _permissions.asStateFlow()

    /** Poziva se na ON_RESUME — korisnik se vraća iz sistemskih podešavanja. */
    fun refreshPermissions() {
        _permissions.value = permissionChecker.snapshot()
        _detoxReadiness.value = detoxReadiness()
    }

    private fun detoxReadiness(): DetoxReadiness {
        val state = screenManagerPermissions.snapshot()
        return DetoxReadiness(
            appsEnforced = state.hasUsageAccess && state.canDrawOverlay,
            sitesEnforced = state.accessibilityEnabled
        )
    }

    fun permissionIntent(kind: AlarmPermission.Kind) = permissionChecker.settingsIntent(kind)

    /** Sledeći trenutak zvonjave alarma (isti račun koji koristi zakazivanje). */
    fun nextTriggerAt(alarm: AlarmRule, now: Long): Long = alarmScheduler.nextTriggerAt(alarm, now)

    fun saveAlarm(alarm: AlarmRule) {
        viewModelScope.launch {
            alarmRepository.upsert(alarm)
            if (alarm.enabled) {
                alarmScheduler.scheduleAlarm(alarm)
            } else {
                alarmScheduler.cancelAlarm(alarm.id)
                runtimeStore.removeSnooze(alarm.id)
            }
        }
    }

    fun setAlarmEnabled(alarm: AlarmRule, enabled: Boolean) {
        saveAlarm(alarm.copy(enabled = enabled))
    }

    fun deleteAlarm(alarmId: Long) {
        viewModelScope.launch {
            alarmRepository.deleteById(alarmId)
            alarmScheduler.cancelAlarm(alarmId)
            runtimeStore.removeSnooze(alarmId)
            WakeCheckController(getApplication()).cancel(alarmId)
        }
    }

    /** Probno zvonjenje sa podešavanjima iz forme (i pre čuvanja). */
    fun testAlarm(draft: AlarmRule) {
        AlarmRingService.test(getApplication(), draft)
    }
}

/**
 * Aplikacije blokira Screen Manager servis (treba Usage access + prikaz preko
 * drugih aplikacija), a sajtove Accessibility servis.
 */
data class DetoxReadiness(val appsEnforced: Boolean, val sitesEnforced: Boolean)
