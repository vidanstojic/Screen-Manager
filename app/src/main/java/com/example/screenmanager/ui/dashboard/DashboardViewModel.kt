package com.example.screenmanager.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.screenmanager.data.local.DailyUsage
import com.example.screenmanager.data.local.HourlyUsage
import com.example.screenmanager.data.local.UsageSummary
import com.example.screenmanager.domain.PermissionState
import com.example.screenmanager.domain.ServiceLocator
import com.example.screenmanager.domain.TimeBuckets
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel koji hrani početni dashboard i usage stats ekran.
 *
 * Svi podaci dolaze iz satnih rollup-ova (usage_hourly); sync pokreće
 * FocusMonitorService, a ovde se radi samo jedan sync pri ulasku da bi
 * ekran bio svež i kada servis još nije startovan.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val permissions = ServiceLocator.permissionStateChecker(application)
    private val runtimeRepository = ServiceLocator.runtimeStateRepository(application)
    private val usageRepository = ServiceLocator.usageStatsRepository(application)

    private val _permissionState = MutableStateFlow(permissions.snapshot())
    val permissionState: StateFlow<PermissionState> = _permissionState

    // --- Izabrani dan u DayPicker-u (Day mod) ---
    // JEDINI izvor istine za izabrani dan.
    private val _selectedDayStart = MutableStateFlow(TimeBuckets.startOfToday())
    val selectedDayStart: StateFlow<Long> = _selectedDayStart

    fun selectDay(dayStart: Long) {
        _selectedDayStart.value = dayStart
    }

    /** Satni podaci UVEK za "danas" (npr. header widget). */
    val hourlyUsage: StateFlow<List<HourlyUsage>> = usageRepository.observeTodayHourlyUsage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Satni podaci za dan izabran u DayPicker-u. */
    val hourlyUsageForSelectedDay: StateFlow<List<HourlyUsage>> = _selectedDayStart
        .flatMapLatest { dayStart -> usageRepository.observeHourlyUsageForDay(dayStart) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Lista aplikacija za dan izabran u DayPicker-u. Ranije je lista uvek
     * prikazivala DANAS, iako je grafik prikazivao izabrani dan.
     */
    val totalsForSelectedDay: StateFlow<List<UsageSummary>> = _selectedDayStart
        .flatMapLatest { dayStart -> usageRepository.observeTotalsForDay(dayStart) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 7-dnevni breakdown (indeks 0..6) za Week prikaz. */
    val weeklyDailyBreakdown: StateFlow<List<DailyUsage>> = usageRepository.observeWeeklyDailyBreakdown()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Po danu za tekući mesec (indeks 0 = 1. u mesecu) — osnova za Month prikaz. */
    val monthlyDailyBreakdown: StateFlow<List<DailyUsage>> = usageRepository.observeMonthlyDailyBreakdown()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dailyTotals: StateFlow<List<UsageSummary>> = usageRepository.observeDailyTotals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weeklyTotals: StateFlow<List<UsageSummary>> = usageRepository.observeWeeklyTotals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val monthlyTotals: StateFlow<List<UsageSummary>> = usageRepository.observeMonthlyTotals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val wakeupBlockedUntil: StateFlow<Long?> = runtimeRepository.observeWakeUpBlockedUntil()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** packageName → kraj Shorts/Reels kazne. */
    val shortsPenalties: StateFlow<Map<String, Long>> = runtimeRepository.observeShortsPenalties()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init {
        refreshUsage()
    }

    fun refreshUsage() {
        viewModelScope.launch {
            if (_permissionState.value.hasUsageAccess) {
                runCatching { usageRepository.syncUsageEvents() }
            }
        }
    }

    /**
     * Osvežava snapshot dozvola (zove se na ON_RESUME ekrana) i, ako je
     * Usage Access upravo dat, odmah povlači istoriju.
     */
    fun refreshPermissions() {
        val previous = _permissionState.value
        val current = permissions.snapshot()
        _permissionState.value = current
        if (!previous.hasUsageAccess && current.hasUsageAccess) refreshUsage()
    }
}
