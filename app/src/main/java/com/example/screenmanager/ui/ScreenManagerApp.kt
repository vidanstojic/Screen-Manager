package com.example.screenmanager.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext // NOVI IMPORT
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.screenmanager.domain.TimeBuckets // NOVI IMPORT
import com.example.screenmanager.domain.toAppUsageSummaries // NOVI IMPORT
import com.example.screenmanager.domain.toHourlyMinutesList // NOVI IMPORT
import com.example.screenmanager.model.AppUsageSummary // NOVI IMPORT
import com.example.screenmanager.model.MainDestination
import com.example.screenmanager.model.UsageRange
import com.example.screenmanager.ui.components.GeneralPlaceholderScreen
import com.example.screenmanager.ui.dashboard.DashboardViewModel
import com.example.screenmanager.ui.dashboard.UsageStatsHomeScreen
import com.example.screenmanager.ui.dashboard.components.DayUiModel
import com.example.screenmanager.ui.details.AppDetailsScreen
import com.example.screenmanager.ui.limits.AddLimitScreen
import com.example.screenmanager.ui.limits.ScheduledBlockScreen
import com.example.screenmanager.ui.limits.UsageLimitsScreen
import com.example.screenmanager.ui.settings.GeneralSettingsScreen
import com.example.screenmanager.ui.FocusFlowHomeScreen
import java.text.SimpleDateFormat // NOVI IMPORT
import java.util.Date // NOVI IMPORT
import java.util.Locale // NOVI IMPORT
import java.util.concurrent.TimeUnit // NOVI IMPORT

/**
 * Glavni Compose router aplikacije.
 *
 * Pošto [MainActivity] samo podiže temu i ovaj composable, ovde se dešava
 * ceo tok ulaska u UI: početni pregled, detalji aplikacije, limiti, editor
 * novog limita, scheduled blocking i settings pregled.
 */
@Composable
fun ScreenManagerApp(viewModel: DashboardViewModel = viewModel()) {
    val context = LocalContext.current

    // --- SKUP PRAVIH PODATAKA IZ VIEWMODELA (NOVO) ---
    val dailyTotals by viewModel.dailyTotals.collectAsState()
    val hourlyUsage by viewModel.hourlyUsage.collectAsState()

    // Mapiramo podatke iz baze u tvoj UI model za listu
    val appsUsage = remember(dailyTotals) {
        dailyTotals.toAppUsageSummaries(context).sortedByDescending { it.minutes }
    }

    var selectedRange by remember { mutableStateOf(UsageRange.Day) }
    // PROMENJENO: Umesto MockUsage.days.last() sada koristimo pravi timestamp za danas
    var selectedDayStart by remember { mutableStateOf(TimeBuckets.startOfToday()) }
    // PROMENJENO: Umesto MockAppUsage sada koristimo tvoj novi AppUsageSummary
    var selectedApp by remember { mutableStateOf<AppUsageSummary?>(null) }
    var selectedDestination by remember { mutableStateOf(MainDestination.UsageStats) }
    // PROMENJENO: Umesto MockAppUsage sada koristimo tvoj novi AppUsageSummary
    var addLimitApp by remember { mutableStateOf<AppUsageSummary?>(null) }

    var showScheduledBlockScreen by remember { mutableStateOf(false) }

    var showFocusFlowHome by remember { mutableStateOf(true) } // default ekran

    if (showFocusFlowHome) {
        FocusFlowHomeScreen(
            onAppDetoxClick = {
                showFocusFlowHome = false
                selectedDestination = MainDestination.UsageStats
            }
        )
        return
    }

    if (showScheduledBlockScreen) {
        ScheduledBlockScreen(
            onBack = { showScheduledBlockScreen = false }
        )
        return
    }

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
            selectedDayStart = selectedDayStart, // PROMENJENO
            selectedDestination = selectedDestination,
            appsUsage = appsUsage, // NOVO: Prosleđujemo pravu mapiranu listu aplikacija
            days = generateLast7DaysUiModels(), // NOVO: Generišemo poslednjih 7 dana

            // NOVO: Podaci za grafikon prilagođeni pravim modelima
            chartTitle = if (selectedRange == UsageRange.Day) "Today's Usage" else "Last 7 Days",
            chartHeadlineMinutes = appsUsage.sumOf { it.minutes }, // Ukupno minuta
            chartPoints = hourlyUsage.toHourlyMinutesList(), // Koristimo maper iz UsageMappers.kt
            chartLabels = listOf("12am", "6am", "Noon", "6pm", "11pm"),
            chartAverage = hourlyUsage.toHourlyMinutesList().average().toFloat().takeIf { !it.isNaN() } ?: 0f,
            chartBottomLabel = "Daily Average: ...",

            onRangeSelected = { selectedRange = it },
            onDaySelected = { selectedDayStart = it }, // PROMENJENO
            onAppClick = { selectedApp = it },
            onDestinationSelected = { selectedDestination = it }
        )

        MainDestination.UsageLimits -> UsageLimitsScreen(
            selectedDestination = selectedDestination,
            onDestinationSelected = { selectedDestination = it },
            onAddLimit = {
                addLimitApp = null
                selectedDestination = MainDestination.AddLimit
            },
            onScheduledBlockClick = {
                showScheduledBlockScreen = true
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

// NOVO: Data klasa za UI prikaz dana u DayPicker-u
data class DayUiModel(
    val timestamp: Long,
    val shortLabel: String,
    val dateLabel: String
)

// NOVO: Funkcija za generisanje poslednjih 7 dana
fun generateLast7DaysUiModels(): List<DayUiModel> {
    val formatter = SimpleDateFormat("d", Locale.getDefault())
    val shortFormatter = SimpleDateFormat("EEE", Locale.getDefault())
    val now = System.currentTimeMillis()
    val startOfToday = TimeBuckets.startOfToday(now)
    val dayMs = TimeUnit.DAYS.toMillis(1)

    // Generiše listu unazad od pre 6 dana do danas (ukupno 7 dana)
    return (6 downTo 0).map { daysAgo ->
        val timestamp = startOfToday - (daysAgo * dayMs)
        val date = Date(timestamp)
        DayUiModel(
            timestamp = timestamp,
            shortLabel = shortFormatter.format(date),
            dateLabel = formatter.format(date)
        )
    }
}