package com.example.screenmanager.ui.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.screenmanager.domain.TimeBuckets
import com.example.screenmanager.domain.toHourlyMinutesList
import com.example.screenmanager.model.*
import com.example.screenmanager.ui.components.BottomNavBar
import com.example.screenmanager.ui.details.components.*
import com.example.screenmanager.ui.theme.DetailBackground
import com.example.screenmanager.ui.theme.PurpleAccent

/**
 * Ekran detalja jedne aplikacije.
 *
 * Do njega se dolazi iz dashboard liste aplikacija. Grafici (satni/dnevni)
 * dolaze iz [AppDetailsViewModel], koji čita realne podatke iz baze preko
 * UsageStatsRepository — ne iz MockUsage.
 *
 * NAPOMENA: selectedDay i dalje koristi MockUsage.days (fiksni datumi,
 * ne stvarni "poslednjih 7 dana"). Ovo je poznat TODO — zajedničko sa
 * UsageStatsHomeScreen-om — rešavamo ga zajedno kasnije kroz novi
 * "RollingDay" model.
 *
 * Statistika u DetailStatsGrid (sesije/trend/prosek) je za sada
 * placeholder — repository trenutno ne izlaže broj sesija po danu.
 */
@Composable
fun AppDetailsScreen(
    app: AppUsageSummary,
    selectedDestination: MainDestination,
    onDestinationSelected: (MainDestination) -> Unit,
    onBack: () -> Unit,
    onAddLimit: () -> Unit,
    viewModel: AppDetailsViewModel = viewModel()
) {
    var selectedRange by remember { mutableStateOf(UsageRange.Day) }
    var selectedDay by remember { mutableStateOf(MockUsage.days.last()) }
    var chartRange by remember { mutableStateOf(UsageRange.Day) }

    // Povezuje ViewModel sa trenutno prikazanom aplikacijom i danom.
    LaunchedEffect(app.packageName) {
        viewModel.selectPackage(app.packageName)
    }
    LaunchedEffect(selectedDay) {
        // MockDayUsage nema stvarni timestamp, pa za sada koristimo
        // startOfToday() kao aproksimaciju dok ne rešimo TODO iznad.
        viewModel.selectDay(TimeBuckets.startOfToday())
    }

    val rawHourlyUsage by viewModel.hourlyUsage.collectAsState()
    val rawDailyUsage by viewModel.dailyUsage.collectAsState()

    val hourlyPoints = remember(rawHourlyUsage) { rawHourlyUsage.toHourlyMinutesList() }
    val dailyPoints = remember(rawDailyUsage) {
        val byDay = rawDailyUsage.associateBy { it.day }
        (0..6).map { day ->
            val ms = byDay[day]?.durationMs ?: 0L
            (ms / 60_000L).toInt()
        }
    }
    val dailyLabels = remember {
        // Isti TODO kao gore — zameniti stvarnim datumima poslednjih 7 dana.
        listOf("D-6", "D-5", "D-4", "D-3", "D-2", "D-1", "Danas")
    }

    // Placeholder statistika dok ne dodamo broj sesija u bazu.
    val details = remember(hourlyPoints, dailyPoints) {
        val totalToday = hourlyPoints.sum()
        val totalWeek = dailyPoints.sum()
        val average = if (dailyPoints.isNotEmpty()) totalWeek / dailyPoints.size else 0
        AppDetailStats(
            usageMinutes = if (selectedRange == UsageRange.Week) totalWeek else totalToday,
            sessions = 0,
            averageMinutes = average,
            previousAverageMinutes = average,
            trendLabel = "—",
            trendColor = Color(0xFF8BE0B0),
            limitStatus = if (totalToday > 60) "High" else "OK"
        )
    }

    Scaffold(
        containerColor = DetailBackground,
        bottomBar = {
            BottomNavBar(
                selected = selectedDestination,
                onSelected = onDestinationSelected
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddLimit,
                containerColor = PurpleAccent,
                contentColor = Color(0xFF211D2B),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("+", fontSize = 26.sp, fontWeight = FontWeight.Medium)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { AppDetailsTopBar(app = app, onBack = onBack) }
            item { AppDetailsTabs() }
            item {
                DetailFilterBar(
                    selectedRange = selectedRange,
                    selectedDay = selectedDay,
                    onRangeSelected = {
                        selectedRange = it
                        chartRange = it
                    },
                    onDaySelected = { selectedDay = it }
                )
            }
            item { DetailStatsGrid(details = details) }
            item {
                Text(
                    text = "Charts",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            item {
                AppDetailsChartCard(
                    app = app,
                    selectedRange = chartRange,
                    hourlyPoints = hourlyPoints,
                    dailyPoints = dailyPoints,
                    dailyLabels = dailyLabels,
                    onToggleRange = {
                        chartRange = if (chartRange == UsageRange.Day) UsageRange.Week else UsageRange.Day
                    }
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "See usage stats for ${app.category}  >",
                        color = PurpleAccent,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}