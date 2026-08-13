package com.example.screenmanager.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.screenmanager.domain.TimeBuckets
import com.example.screenmanager.domain.generateLastSevenDays
import com.example.screenmanager.domain.toAppUsageSummaries
import com.example.screenmanager.domain.toHourlyMinutesList
import com.example.screenmanager.model.AppUsageSummary
import com.example.screenmanager.model.MainDestination
import com.example.screenmanager.model.UsageRange
import com.example.screenmanager.ui.alarms.SmartAlarmsScreen
import com.example.screenmanager.ui.components.GeneralPlaceholderScreen
import com.example.screenmanager.ui.components.SwipeBackContainer
import com.example.screenmanager.ui.dashboard.DashboardViewModel
import com.example.screenmanager.ui.dashboard.UsageStatsHomeScreen
import com.example.screenmanager.ui.details.AppDetailsScreen
import com.example.screenmanager.ui.limits.AddLimitScreen
import com.example.screenmanager.ui.limits.ScheduledBlockScreen
import com.example.screenmanager.ui.limits.UsageLimitsScreen
import com.example.screenmanager.ui.settings.GeneralSettingsScreen

/**
 * Svi mogući "ekrani" u aplikaciji. Back-stack (List<Screen>) je JEDINI
 * izvor istine za navigaciju - i sistemski back i swipe-back gest rade
 * nad istim stack-om preko iste navigateBack() funkcije.
 */
private sealed class Screen {
    object FocusFlowHome : Screen()
    object NavigationHub : Screen()
    object ScheduledBlock : Screen()
    object SmartAlarms : Screen()
    data class Destination(val destination: MainDestination) : Screen()
    data class AppDetails(val app: AppUsageSummary) : Screen()
    data class AddLimit(val app: AppUsageSummary?) : Screen()
}

@Composable
fun ScreenManagerApp(viewModel: DashboardViewModel = viewModel()) {
    val context = LocalContext.current

    val dailyTotals by viewModel.dailyTotals.collectAsState()
    val hourlyUsage by viewModel.hourlyUsage.collectAsState()

    val appsUsage = remember(dailyTotals) {
        dailyTotals.toAppUsageSummaries(context).sortedByDescending { it.minutes }
    }

    var selectedRange by remember { mutableStateOf(UsageRange.Day) }
    var selectedDayStart by remember { mutableStateOf(TimeBuckets.startOfToday()) }

    // Koren stack-a je UVEK FocusFlowHome - to je jedini ekran sa kog
    // back izlazi iz aplikacije.
    val backStack = remember { mutableStateListOf<Screen>(Screen.FocusFlowHome) }
    val currentScreen = backStack.last()

    fun navigateTo(screen: Screen) {
        backStack.add(screen)
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
        // size == 1 znači da smo na FocusFlowHome - tu ne radimo ništa,
        // BackHandler ispod je tada isključen pa sistem sam izađe iz app-a.
    }

    // Sistemski (hardverski/gesture) back taster telefona.
    // Isključen SAMO na FocusFlowHome -> tamo Android sam izlazi iz aplikacije.
    BackHandler(enabled = backStack.size > 1) {
        navigateBack()
    }

    val swipeBackEnabled = backStack.size > 1

    when (val screen = currentScreen) {

        Screen.FocusFlowHome -> {
            FocusFlowHomeScreen(
                onAppDetoxClick = { navigateTo(Screen.NavigationHub) },
                onSmartAlarmsClick = { navigateTo(Screen.SmartAlarms) }
            )
        }

        Screen.NavigationHub -> {
            SwipeBackContainer(onBack = ::navigateBack, enabled = swipeBackEnabled) {
                NavigationHubScreen(
                    onUsageStatsClick = { navigateTo(Screen.Destination(MainDestination.UsageStats)) },
                    onUsageLimitsClick = { navigateTo(Screen.Destination(MainDestination.UsageLimits)) },
                    onGeneralUsageClick = { navigateTo(Screen.Destination(MainDestination.GeneralUsage)) },
                    onGeneralSettingsClick = { navigateTo(Screen.Destination(MainDestination.GeneralSettings)) }
                )
            }
        }

        Screen.SmartAlarms -> {
            SwipeBackContainer(onBack = ::navigateBack, enabled = swipeBackEnabled) {
                SmartAlarmsScreen(onBack = ::navigateBack)
            }
        }

        Screen.ScheduledBlock -> {
            SwipeBackContainer(onBack = ::navigateBack, enabled = swipeBackEnabled) {
                ScheduledBlockScreen(onBack = ::navigateBack)
            }
        }

        is Screen.AddLimit -> {
            SwipeBackContainer(onBack = ::navigateBack, enabled = swipeBackEnabled) {
                AddLimitScreen(
                    selectedApp = screen.app,
                    onBack = ::navigateBack,
                    onCancel = ::navigateBack,
                    onSave = ::navigateBack
                )
            }
        }

        is Screen.AppDetails -> {
            SwipeBackContainer(onBack = ::navigateBack, enabled = swipeBackEnabled) {
                AppDetailsScreen(
                    app = screen.app,
                    selectedDestination = MainDestination.UsageStats,
                    onDestinationSelected = { navigateTo(Screen.Destination(it)) },
                    onBack = ::navigateBack,
                    onAddLimit = { navigateTo(Screen.AddLimit(screen.app)) }
                )
            }
        }

        is Screen.Destination -> {
            SwipeBackContainer(onBack = ::navigateBack, enabled = swipeBackEnabled) {
                when (screen.destination) {
                    MainDestination.UsageStats -> UsageStatsHomeScreen(
                        selectedRange = selectedRange,
                        selectedDayStart = selectedDayStart,
                        selectedDestination = screen.destination,
                        appsUsage = appsUsage,
                        days = generateLastSevenDays(),
                        chartTitle = if (selectedRange == UsageRange.Day) "Today's Usage" else "Last 7 Days",
                        chartHeadlineMinutes = appsUsage.sumOf { it.minutes },
                        chartPoints = hourlyUsage.toHourlyMinutesList(),
                        chartLabels = listOf("12am", "6am", "Noon", "6pm", "11pm"),
                        chartAverage = hourlyUsage.toHourlyMinutesList().average().toFloat().takeIf { !it.isNaN() } ?: 0f,
                        chartBottomLabel = "Daily Average: ...",
                        onRangeSelected = { selectedRange = it },
                        onDaySelected = { selectedDayStart = it },
                        onAppClick = { navigateTo(Screen.AppDetails(it)) },
                        onDestinationSelected = { navigateTo(Screen.Destination(it)) }
                    )

                    MainDestination.UsageLimits -> UsageLimitsScreen(
                        selectedDestination = screen.destination,
                        onDestinationSelected = { navigateTo(Screen.Destination(it)) },
                        onAddLimit = { navigateTo(Screen.AddLimit(null)) },
                        onScheduledBlockClick = { navigateTo(Screen.ScheduledBlock) }
                    )

                    MainDestination.GeneralUsage -> GeneralSettingsScreen(
                        destination = screen.destination,
                        onDestinationSelected = { navigateTo(Screen.Destination(it)) }
                    )

                    MainDestination.GeneralSettings -> GeneralPlaceholderScreen(
                        destination = screen.destination,
                        title = "General Settings",
                        description = "Ovde ćemo kasnije prikazati globalne obrasce korišćenja, kategorije i dnevne rutine.",
                        onDestinationSelected = { navigateTo(Screen.Destination(it)) }
                    )

                    MainDestination.AddLimit -> Unit
                }
            }
        }
    }
}