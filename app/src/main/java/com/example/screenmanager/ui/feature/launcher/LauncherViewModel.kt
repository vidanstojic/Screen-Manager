package com.example.screenmanager.ui.feature.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.screenmanager.domain.ServiceLocator
import com.example.screenmanager.domain.TimeBuckets
import com.example.screenmanager.ui.common.tickerFlow
import com.example.screenmanager.ui.feature.limits.toProtectionSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Kratak status oba dela aplikacije, prikazan na njihovim karticama. */
data class LauncherUiState(
    /** Screen Manager: ukupno vreme ekrana danas. */
    val todayMs: Long = 0L,
    /** Screen Manager: broj uključenih pravila svih vrsta. */
    val activeRules: Int = 0,
    /** Smart Alarms: trenutak sledećeg uključenog alarma, ili null. */
    val nextAlarmAt: Long? = null,
    /** Smart Alarms: ukupan broj alarma (uključenih i isključenih). */
    val alarmCount: Int = 0
)

/**
 * ViewModel početnog ekrana. Jedino mesto u UI-u koje čita podatke OBA dela
 * aplikacije — sami delovi (Screen Manager, Smart Alarms) ne znaju jedan za drugi.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val usageRepository = ServiceLocator.usageStatsRepository(application)
    private val settings = ServiceLocator.settingsRepository(application)
    private val alarmRepository = ServiceLocator.alarmRepository(application)
    private val alarmScheduler = ServiceLocator.alarmScheduler(application)

    private val today = MutableStateFlow(TimeBuckets.startOfToday())

    private val todayMs = today
        .flatMapLatest { usageRepository.observeTotalsForDay(it) }
        .map { totals -> totals.sumOf { it.totalDurationMs } }

    val uiState: StateFlow<LauncherUiState> = combine(
        todayMs,
        settings.observeRulesSnapshot(),
        alarmRepository.observeAll(),
        // "Sledeći alarm" zavisi od trenutnog vremena.
        tickerFlow()
    ) { todayMs, rules, alarms, now ->
        LauncherUiState(
            todayMs = todayMs,
            activeRules = rules.toProtectionSummary().activeCount,
            nextAlarmAt = alarms.filter { it.enabled }.minOfOrNull { alarmScheduler.nextTriggerAt(it, now) },
            alarmCount = alarms.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LauncherUiState())

    /** Posle ponoći pređi na novi dan (ViewModel živi koliko i aktivnost). */
    fun onResume() {
        today.value = TimeBuckets.startOfToday()
    }
}
