package com.example.screenmanager.ui.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.screenmanager.domain.ServiceLocator
import com.example.screenmanager.domain.TimeBuckets
import com.example.screenmanager.domain.rules.RulesSnapshot
import com.example.screenmanager.model.AlarmRule
import com.example.screenmanager.model.ShortsMode
import com.example.screenmanager.ui.common.tickerFlow
import com.example.screenmanager.ui.model.AppUsageItem
import com.example.screenmanager.ui.model.toHourlyMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/** Koliko pravila svake vrste je trenutno uključeno. */
data class ProtectionSummary(
    val dailyLimits: Int = 0,
    val sessionLimits: Int = 0,
    val schedules: Int = 0,
    /** Mod Shorts/Reels pravila, ili null ako je isključeno. */
    val shortsMode: ShortsMode? = null,
    val morningLockEnabled: Boolean = false
)

data class HomeUiState(
    val todayMs: Long = 0L,
    /** Potrošnja juče do istog doba dana (za poređenje); null ako juče nema podataka. */
    val yesterdaySameTimeMs: Long? = null,
    /** Sati 0..23 današnjeg dana, u ms (mini grafik). */
    val hourlyMs: List<Long> = List(24) { 0L },
    val topApps: List<AppUsageItem> = emptyList(),
    val protection: ProtectionSummary = ProtectionSummary(),
    /** Kraj aktivne emergency pauze, ili null. */
    val emergencyPauseUntil: Long? = null,
    /** Kraj aktivne jutarnje blokade, ili null. */
    val morningLockUntil: Long? = null,
    /** Trenutak sledećeg uključenog alarma, ili null. */
    val nextAlarmAt: Long? = null
)

/**
 * ViewModel Home ekrana: sažetak današnje potrošnje, stanja zaštite i
 * sledećeg alarma. Sve vrednosti su stvarni podaci iz repozitorijuma.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val usageRepository = ServiceLocator.usageStatsRepository(application)
    private val settings = ServiceLocator.settingsRepository(application)
    private val runtimeState = ServiceLocator.runtimeStateRepository(application)
    private val alarmRepository = ServiceLocator.alarmRepository(application)
    private val alarmScheduler = ServiceLocator.alarmScheduler(application)
    private val installedApps = ServiceLocator.installedApps(application)

    private val today = MutableStateFlow(TimeBuckets.startOfToday())

    private class Usage(
        val todayHourlyMs: List<Long>,
        val yesterdayHourlyMs: List<Long>,
        val topApps: List<AppUsageItem>
    )

    private val usage: Flow<Usage> = today.flatMapLatest { todayStart ->
        val yesterdayStart = TimeBuckets.startOfDay(TimeBuckets.epochDay(todayStart) - 1)
        combine(
            usageRepository.observeHourlyUsageForDay(todayStart),
            usageRepository.observeHourlyUsageForDay(yesterdayStart),
            usageRepository.observeTotalsForDay(todayStart)
        ) { todayHourly, yesterdayHourly, totals ->
            Usage(
                todayHourlyMs = todayHourly.toHourlyMs(),
                yesterdayHourlyMs = yesterdayHourly.toHourlyMs(),
                topApps = totals
                    .filter { it.totalDurationMs > 0 }
                    .sortedByDescending { it.totalDurationMs }
                    .take(TOP_APPS_COUNT)
                    .map { AppUsageItem(it.packageName, installedApps.label(it.packageName), it.totalDurationMs) }
            )
        }
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<HomeUiState> = combine(
        usage,
        settings.observeRulesSnapshot(),
        runtimeState.observeWakeUpBlockedUntil(),
        alarmRepository.observeAll(),
        // Poređenje sa jučerašnjim danom i "aktivno do" zavise od trenutnog vremena.
        tickerFlow()
    ) { usage, rules, morningLockUntil, alarms, now ->
        HomeUiState(
            todayMs = usage.todayHourlyMs.sum(),
            yesterdaySameTimeMs = usageUntilTimeOfDay(usage.yesterdayHourlyMs, now).takeIf { it > 0 },
            hourlyMs = usage.todayHourlyMs,
            topApps = usage.topApps,
            protection = rules.toProtectionSummary(),
            emergencyPauseUntil = rules.emergency?.activeUntilMillis?.takeIf { it > now },
            morningLockUntil = morningLockUntil?.takeIf { it > now },
            nextAlarmAt = nextAlarmAt(alarms, now)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Posle ponoći pređi na novi dan (ViewModel živi koliko i aktivnost). */
    fun onResume() {
        today.value = TimeBuckets.startOfToday()
    }

    fun endEmergencyPause() {
        viewModelScope.launch {
            val config = settings.getEmergencySessionConfig() ?: return@launch
            settings.updateEmergencySessionConfig(config.ended())
        }
    }

    private fun RulesSnapshot.toProtectionSummary() = ProtectionSummary(
        dailyLimits = appLimits.count { it.isEnabled },
        sessionLimits = sessionLimits.count { it.isEnabled },
        schedules = schedules.count { it.isEnabled },
        shortsMode = shortVideo?.takeIf { it.isEnabled && it.selectedAppIds.isNotEmpty() }?.mode,
        morningLockEnabled = wakeUp?.let { it.isEnabled && it.selectedAppIds.isNotEmpty() } ?: false
    )

    private fun nextAlarmAt(alarms: List<AlarmRule>, now: Long): Long? =
        alarms.filter { it.enabled }.minOfOrNull { alarmScheduler.nextTriggerAt(it, now) }

    /** Zbir sati pre trenutnog doba dana + srazmeran deo tekućeg sata. */
    private fun usageUntilTimeOfDay(hourlyMs: List<Long>, now: Long): Long {
        val time = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalTime()
        val fullHours = hourlyMs.take(time.hour).sum()
        val partialHour = (hourlyMs.getOrElse(time.hour) { 0L } * time.minute) / 60
        return fullHours + partialHour
    }

    private companion object {
        const val TOP_APPS_COUNT = 3
    }
}
