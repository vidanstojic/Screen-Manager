package com.example.screenmanager.ui.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.screenmanager.data.preferences.UiPreferences
import com.example.screenmanager.domain.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** ViewModel Settings ekrana: podešavanja prikaza (dnevni cilj). Dozvole drži AppViewModel. */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = ServiceLocator.uiPreferences(application)

    /** Dnevni cilj u minutima, ili `null` ako nije postavljen. */
    val dailyGoalMinutes: StateFlow<Int?> = preferences.observeDailyGoalMinutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), preferences.dailyGoalMinutes())

    fun setDailyGoalEnabled(enabled: Boolean) {
        preferences.setDailyGoalMinutes(if (enabled) UiPreferences.DEFAULT_GOAL_MINUTES else null)
    }

    fun setDailyGoalMinutes(minutes: Int) {
        preferences.setDailyGoalMinutes(minutes)
    }
}
