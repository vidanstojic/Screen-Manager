package com.example.screenmanager.ui.feature.alarms

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.screenmanager.alarm.AlarmModule
import com.example.screenmanager.alarm.data.AlarmPermission
import com.example.screenmanager.alarm.data.AlarmPermissions
import com.example.screenmanager.alarm.data.PendingSnooze
import com.example.screenmanager.alarm.service.AlarmRingService
import com.example.screenmanager.domain.ServiceLocator
import com.example.screenmanager.model.AlarmRule
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

    /** `null` dok se alarmi prvi put ne učitaju iz baze. */
    val alarms: StateFlow<List<AlarmRule>?> = alarmRepository.observeAll()
        .map { list -> list.sortedWith(compareBy({ it.hour }, { it.minute })) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Odloženi alarmi (po id-ju) — lista prikazuje "Snoozed until". */
    val snoozes: StateFlow<Map<Long, PendingSnooze>> = runtimeStore.snoozes

    private val _permissions = MutableStateFlow(permissionChecker.snapshot())
    val permissions: StateFlow<List<AlarmPermission>> = _permissions.asStateFlow()

    /** Poziva se na ON_RESUME — korisnik se vraća iz sistemskih podešavanja. */
    fun refreshPermissions() {
        _permissions.value = permissionChecker.snapshot()
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
        }
    }

    /** Probno zvonjenje sa podešavanjima iz forme (i pre čuvanja). */
    fun testAlarm(draft: AlarmRule) {
        AlarmRingService.test(getApplication(), draft)
    }
}
