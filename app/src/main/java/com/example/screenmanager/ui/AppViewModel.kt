package com.example.screenmanager.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.screenmanager.domain.PermissionState
import com.example.screenmanager.domain.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Stanje koje deli cela aplikacija: sistemske dozvole.
 *
 * [onResume] se zove svaki put kada aplikacija dođe u prvi plan: osvežava
 * dozvole (korisnik se obično vraća iz sistemskih podešavanja) i povlači
 * nove događaje potrošnje, da ekrani budu sveži i kada servis još ne radi.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val permissions = ServiceLocator.permissionStateChecker(application)
    private val usageRepository = ServiceLocator.usageStatsRepository(application)

    private val _permissionState = MutableStateFlow(permissions.snapshot())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    fun onResume() {
        val current = permissions.snapshot()
        _permissionState.value = current
        if (current.hasUsageAccess) {
            viewModelScope.launch { runCatching { usageRepository.syncUsageEvents() } }
        }
    }
}
