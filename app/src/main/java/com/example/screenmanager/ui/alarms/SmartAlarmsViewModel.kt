package com.example.screenmanager.ui.alarms

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.screenmanager.domain.ServiceLocator
import com.example.screenmanager.model.AlarmRule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SmartAlarmsViewModel(application: Application) : AndroidViewModel(application) {
    private val alarmRepository = ServiceLocator.alarmRepository(application)
    private val alarmScheduler = ServiceLocator.alarmScheduler(application)

    val alarms: StateFlow<List<AlarmRule>> = alarmRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    fun toggleAlarm(alarmId: Long, enabled: Boolean) {
        val alarm = alarms.value.find { it.id == alarmId } ?: return
        saveAlarm(alarm.copy(enabled = enabled))
    }

    fun deleteAlarm(alarmId: Long) {
        viewModelScope.launch {
            alarmRepository.deleteById(alarmId)
            alarmScheduler.cancelAlarm(alarmId)
        }
    }
}
