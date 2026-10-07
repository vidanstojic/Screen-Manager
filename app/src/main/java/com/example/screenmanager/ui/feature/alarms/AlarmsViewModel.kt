package com.example.screenmanager.ui.feature.alarms

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.screenmanager.domain.ServiceLocator
import com.example.screenmanager.model.AlarmRule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    /** `null` dok se alarmi prvi put ne učitaju iz baze. */
    val alarms: StateFlow<List<AlarmRule>?> = alarmRepository.observeAll()
        .map { list -> list.sortedWith(compareBy({ it.hour }, { it.minute })) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Sledeći trenutak zvonjave alarma (isti račun koji koristi zakazivanje). */
    fun nextTriggerAt(alarm: AlarmRule, now: Long): Long = alarmScheduler.nextTriggerAt(alarm, now)

    fun saveAlarm(alarm: AlarmRule) {
        viewModelScope.launch {
            alarmRepository.upsert(alarm)
            if (alarm.enabled) {
                alarmScheduler.scheduleAlarm(alarm)
            } else {
                alarmScheduler.cancelAlarm(alarm.id)
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
        }
    }
}
