package com.example.screenmanager.ui

import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.screenmanager.ui.theme.ScreenManagerGlassTheme
import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import com.example.screenmanager.domain.toDailyMinutesList

/**
 * Svi mogući "ekrani" u aplikaciji. Back-stack (List<Screen>) je JEDINI
 * izvor istine za navigaciju - i sistemski back i swipe-back gest rade
 * nad istim stack-om preko iste navigateBack() funkcije.
 */
private sealed class Screen : Parcelable {
    @Parcelize object FocusFlowHome : Screen()
    @Parcelize object NavigationHub : Screen()
    @Parcelize object ScheduledBlock : Screen()
    @Parcelize object SmartAlarms : Screen()
    @Parcelize data class Destination(val destination: MainDestination) : Screen()
    @Parcelize data class AppDetails(val app: AppUsageSummary) : Screen()
    @Parcelize data class AddLimit(val app: AppUsageSummary?) : Screen()
}

/**
 * Saver koji SnapshotStateList<Screen> pretvara u običnu List<Screen> za čuvanje
 * (svaki Screen je već Parcelable, pa se lista bez problema upakuje u Bundle),
 * i nazad u SnapshotStateList pri obnavljanju (npr. posle rotacije ekrana).
 */
private fun screenBackStackSaver(): Saver<SnapshotStateList<Screen>, *> = listSaver(
    save = { stateList -> stateList.toList() },
    restore = { savedList -> savedList.toMutableStateList() }
)

@Composable
fun ScreenManagerApp(viewModel: DashboardViewModel = viewModel()) {
    ScreenManagerGlassTheme(darkTheme = true) {
        val context = LocalContext.current

        val totalsForSelectedDay by viewModel.totalsForSelectedDay.collectAsState()
        val hourlyUsageForSelectedDay by viewModel.hourlyUsageForSelectedDay.collectAsState()
        val weeklyDailyBreakdown by viewModel.weeklyDailyBreakdown.collectAsState()
        val selectedDayStart by viewModel.selectedDayStart.collectAsState()
        val permissionState by viewModel.permissionState.collectAsState()

        // Po povratku iz sistemskih podešavanja osveži dozvole (i povuci
        // istoriju ako je Usage Access upravo dat).
        @Suppress("DEPRECATION")
        val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissions()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        // Lista aplikacija prati dan izabran u DayPicker-u (ranije uvek "danas").
        val appsUsage = remember(totalsForSelectedDay) {
            totalsForSelectedDay.toAppUsageSummaries(context).sortedByDescending { it.minutes }
        }

        var selectedRange by remember { mutableStateOf(UsageRange.Day) }
        // Koren stack-a je UVEK FocusFlowHome - to je jedini ekran sa kog
        // back izlazi iz aplikacije.
        val backStack = rememberSaveable(saver = screenBackStackSaver()) {
            mutableStateListOf<Screen>(Screen.FocusFlowHome)
        }
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
                        permissionState = permissionState,
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
                        MainDestination.UsageStats -> {
                            val isDayMode = selectedRange == UsageRange.Day
                            val dayChartPoints = hourlyUsageForSelectedDay.toHourlyMinutesList()
                            val weekChartPoints = weeklyDailyBreakdown.toDailyMinutesList()
                            val chartPoints = if (isDayMode) dayChartPoints else weekChartPoints
                            val chartLabels = if (isDayMode) {
                                listOf("12am", "6am", "Noon", "6pm", "11pm")
                            } else {
                                generateLastSevenDays().map { it.shortLabel }
                            }

                            UsageStatsHomeScreen(
                                selectedRange = selectedRange,
                                selectedDayStart = selectedDayStart,
                                selectedDestination = screen.destination,
                                appsUsage = appsUsage,
                                days = generateLastSevenDays(),
                                chartTitle = when {
                                    !isDayMode -> "Last 7 Days"
                                    selectedDayStart == TimeBuckets.startOfToday() -> "Today's Usage"
                                    else -> "Daily Usage"
                                },
                                chartHeadlineMinutes = chartPoints.sum(),
                                chartPoints = chartPoints,
                                chartLabels = chartLabels,
                                chartAverage = chartPoints.average().toFloat()
                                    .takeIf { !it.isNaN() } ?: 0f,
                                chartBottomLabel = "Daily Average: ...",
                                onRangeSelected = { selectedRange = it },
                                onDaySelected = { viewModel.selectDay(it) },
                                onAppClick = { navigateTo(Screen.AppDetails(it)) },
                                onDestinationSelected = { navigateTo(Screen.Destination(it)) }
                            )
                        }

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
}