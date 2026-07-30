package com.example.screenmanager.ui

import androidx.compose.runtime.*
import com.example.screenmanager.model.MainDestination
import com.example.screenmanager.model.MockAppUsage
import com.example.screenmanager.model.MockUsage
import com.example.screenmanager.model.UsageRange
import com.example.screenmanager.ui.components.GeneralPlaceholderScreen
import com.example.screenmanager.ui.dashboard.DashboardViewModel
import com.example.screenmanager.ui.dashboard.UsageStatsHomeScreen
import com.example.screenmanager.ui.details.AppDetailsScreen
import com.example.screenmanager.ui.limits.AddLimitScreen
import com.example.screenmanager.ui.limits.UsageLimitsScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.screenmanager.ui.settings.GeneralSettingsScreen

// Importuj sve potrebne klase (MockUsage, MainDestination, UsageRange, itd.)
// Importuj ekrane iz dashboard i limits paketa

@Composable
fun ScreenManagerApp(viewModel: DashboardViewModel = viewModel()) {
    var selectedRange by remember { mutableStateOf(UsageRange.Day) }
    var selectedDay by remember { mutableStateOf(MockUsage.days.last()) }
    var selectedApp by remember { mutableStateOf<MockAppUsage?>(null) }
    var selectedDestination by remember { mutableStateOf(MainDestination.UsageStats) }
    var addLimitApp by remember { mutableStateOf<MockAppUsage?>(null) }

    if (selectedDestination == MainDestination.AddLimit) {
        AddLimitScreen(
            selectedApp = addLimitApp,
            onBack = { selectedDestination = MainDestination.UsageLimits },
            onCancel = { selectedDestination = MainDestination.UsageLimits },
            onSave = { selectedDestination = MainDestination.UsageLimits }
        )
        return
    }

    if (selectedApp != null) {
        AppDetailsScreen(
            app = selectedApp!!,
            selectedDestination = selectedDestination,
            onDestinationSelected = {
                selectedDestination = it
                selectedApp = null
            },
            onBack = { selectedApp = null },
            onAddLimit = {
                addLimitApp = selectedApp
                selectedDestination = MainDestination.AddLimit
            }
        )
        return
    }

    when (selectedDestination) {
        MainDestination.UsageStats -> UsageStatsHomeScreen(
            selectedRange = selectedRange,
            selectedDay = selectedDay,
            selectedDestination = selectedDestination,
            onRangeSelected = { selectedRange = it },
            onDaySelected = { selectedDay = it },
            onAppClick = { selectedApp = it },
            onDestinationSelected = { selectedDestination = it }
        )

        MainDestination.UsageLimits -> UsageLimitsScreen(
            selectedDestination = selectedDestination,
            onDestinationSelected = { selectedDestination = it },
            onAddLimit = {
                addLimitApp = null
                selectedDestination = MainDestination.AddLimit
            }
        )

        MainDestination.GeneralUsage -> {
            GeneralSettingsScreen(
                destination = selectedDestination,
                onDestinationSelected = { selectedDestination = it }
            )
        }

        MainDestination.GeneralSettings -> GeneralPlaceholderScreen(
            destination = selectedDestination,
            title = "General Settings",
            description = "Ovde ćemo kasnije prikazati globalne obrasce korišćenja, kategorije i dnevne rutine.",
            onDestinationSelected = { selectedDestination = it }
        )

        MainDestination.AddLimit -> Unit
    }
}