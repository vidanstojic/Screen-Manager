package com.example.screenmanager.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.screenmanager.data.local.BlockConfig
import com.example.screenmanager.data.local.HourlyUsage
import com.example.screenmanager.data.local.UsageSummary
import com.example.screenmanager.domain.PermissionState
import com.example.screenmanager.domain.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val permissions = ServiceLocator.permissionStateChecker(application)
    private val blockRepository = ServiceLocator.blockRepository(application)
    private val usageRepository = ServiceLocator.usageStatsRepository(application)

    private val _permissionState = MutableStateFlow(permissions.snapshot())
    val permissionState: StateFlow<PermissionState> = _permissionState

    val configs: StateFlow<List<BlockConfig>> = blockRepository.observeConfigs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val hourlyUsage: StateFlow<List<HourlyUsage>> = usageRepository.observeTodayHourlyUsage()
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

    fun refreshPermissions() {
        _permissionState.value = permissions.snapshot()
    }

    fun setWakeupBlocked(config: BlockConfig, enabled: Boolean) {
        viewModelScope.launch {
            blockRepository.upsertConfig(config.copy(isBlockedDuringWakeup = enabled))
        }
    }
}
