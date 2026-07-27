package com.example.screenmanager.ui.settings

import androidx.lifecycle.ViewModel
import com.example.screenmanager.model.ScheduleRule
import com.example.screenmanager.model.ShortVideoConfig
import com.example.screenmanager.model.WakeUpConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

class SettingsViewModel : ViewModel() {

    private val _wakeUpConfig = MutableStateFlow(WakeUpConfig())
    val wakeUpConfig = _wakeUpConfig.asStateFlow()

    private val _shortVideoConfig = MutableStateFlow(ShortVideoConfig())
    val shortVideoConfig = _shortVideoConfig.asStateFlow()

    private val _scheduleRules = MutableStateFlow<List<ScheduleRule>>(emptyList())
    val scheduleRules = _scheduleRules.asStateFlow()

    fun updateWakeUpConfig(config: WakeUpConfig) {
        _wakeUpConfig.value = config
    }

    fun updateShortVideoConfig(config: ShortVideoConfig) {
        _shortVideoConfig.value = config
    }

    fun toggleScheduleRule(ruleId: String, isEnabled: Boolean) {
        _scheduleRules.update { rules ->
            rules.map { if (it.id == ruleId) it.copy(isEnabled = isEnabled) else it }
        }
    }

    // Mock funkcija za dodavanje novog pravila
    fun addMockScheduleRule() {
        val newRule = ScheduleRule(
            id = UUID.randomUUID().toString(),
            name = "Novo pravilo",
            startTime = LocalTime.of(22, 0),
            endTime = LocalTime.of(7, 0),
            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)
        )
        _scheduleRules.update { it + newRule }
    }
}