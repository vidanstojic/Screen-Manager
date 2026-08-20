package com.example.screenmanager.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.screenmanager.data.local.BlockConfig
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
 * ViewModel koji hrani početni dashboard i usage stats ekran stvarnim
 * podacima i statusima.
 *
 * Odavde UI dobija dozvole, usage zbirke i blok konfiguracije za pregled
 * odmah po ulasku u aplikaciju.
 */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val permissions = ServiceLocator.permissionStateChecker(application)
    private val blockRepository = ServiceLocator.blockRepository(application)
    private val usageRepository = ServiceLocator.usageStatsRepository(application)

    private val _permissionState = MutableStateFlow(permissions.snapshot())
    val permissionState: StateFlow<PermissionState> = _permissionState

    val configs: StateFlow<List<BlockConfig>> = blockRepository.observeConfigs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- Izabrani dan u DayPicker-u (Day mod) ---
    // Ovo je JEDINI izvor istine za izabrani dan — UI ne sme da drži svoju
    // lokalnu kopiju, jer se onda desinhronizuje sa podacima koji se povlače.
    private val _selectedDayStart = MutableStateFlow(TimeBuckets.startOfToday())
    val selectedDayStart: StateFlow<Long> = _selectedDayStart

    fun selectDay(dayStart: Long) {
        _selectedDayStart.value = dayStart
    }

    // Satni podaci UVEK za "danas" — koristi se tamo gde eksplicitno treba
    // današnji prikaz bez obzira na DayPicker (npr. header widget).
    val hourlyUsage: StateFlow<List<HourlyUsage>> = usageRepository.observeTodayHourlyUsage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // NOVO — ovo je bio nedostajući deo: satni podaci koji prate
    // selectedDayStart, tako da DayPicker stvarno menja ono što se prikazuje.
    @OptIn(ExperimentalCoroutinesApi::class)
    val hourlyUsageForSelectedDay: StateFlow<List<HourlyUsage>> = _selectedDayStart
        .flatMapLatest { dayStart -> usageRepository.observeHourlyUsageForDay(dayStart) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // NOVO — 7-dnevni breakdown za Week prikaz na glavnom chart-u.
    // Ranije se ovo nigde nije povlačilo, pa je Week mod tiho prikazivao
    // satne (0..23) podatke umesto dnevnih (0..6).
    val weeklyDailyBreakdown: StateFlow<List<DailyUsage>> = usageRepository.observeWeeklyDailyBreakdown()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dailyTotals: StateFlow<List<UsageSummary>> = usageRepository.observeDailyTotals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weeklyTotals: StateFlow<List<UsageSummary>> = usageRepository.observeWeeklyTotals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val monthlyTotals: StateFlow<List<UsageSummary>> = usageRepository.observeMonthlyTotals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val wakeupBlockedUntil: StateFlow<Long?> = blockRepository.observeWakeupBlockedUntil()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val shortsBlockedUntil: StateFlow<Long?> = blockRepository.observeShortsBlockedUntil()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            blockRepository.seedDefaultsIfNeeded()
            runCatching { usageRepository.syncUsageEvents() }
        }
    }

    /**
     * Osvežava trenutni snapshot dozvola iz sistemskih podešavanja.
     */
    fun refreshPermissions() {
        _permissionState.value = permissions.snapshot()
    }

    /**
     * Menja wake-up blok u local store-u.
     */
    fun setWakeupBlocked(config: BlockConfig, enabled: Boolean) {
        viewModelScope.launch {
            blockRepository.upsertConfig(config.copy(isBlockedDuringWakeup = enabled))
        }
    }
}