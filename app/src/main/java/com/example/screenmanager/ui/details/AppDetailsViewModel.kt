package com.example.screenmanager.ui.details

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.screenmanager.data.local.DailyUsage
import com.example.screenmanager.data.local.HourlyUsage
import com.example.screenmanager.domain.ServiceLocator
import com.example.screenmanager.domain.TimeBuckets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel za ekran detalja jedne aplikacije.
 *
 * Prima packageName preko [selectPackage] i izlaže satnu/dnevnu potrošnju
 * baš za tu aplikaciju, umesto zbira svih aplikacija kao DashboardViewModel.
 */
class AppDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val usageRepository = ServiceLocator.usageStatsRepository(application)

    private val _packageName = MutableStateFlow<String?>(null)
    private val _selectedDayStart = MutableStateFlow(TimeBuckets.startOfToday())

    val hourlyUsage: StateFlow<List<HourlyUsage>> = kotlinx.coroutines.flow.combine(
        _packageName, _selectedDayStart
    ) { pkg, dayStart -> pkg to dayStart }
        .flatMapLatest { (pkg, dayStart) ->
            if (pkg == null) kotlinx.coroutines.flow.flowOf(emptyList())
            else usageRepository.observeHourlyUsageForApp(pkg, dayStart)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dailyUsage: StateFlow<List<DailyUsage>> = _packageName
        .flatMapLatest { pkg ->
            if (pkg == null) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                usageRepository.observeDailyUsageForApp(pkg)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Postavlja koju aplikaciju ovaj ekran trenutno prikazuje.
     * Zove se odmah po ulasku na ekran (npr. iz LaunchedEffect).
     */
    fun selectPackage(packageName: String) {
        _packageName.value = packageName
    }

    /**
     * Menja dan za koji se prikazuje satna raspodela (Day Picker unutar detalja).
     */
    fun selectDay(dayStartTimestamp: Long) {
        _selectedDayStart.value = dayStartTimestamp
    }
}