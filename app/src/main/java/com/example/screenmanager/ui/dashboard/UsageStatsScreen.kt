package com.example.screenmanager.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.screenmanager.model.*
import com.example.screenmanager.ui.components.BottomNavBar
import com.example.screenmanager.ui.dashboard.components.*
import com.example.screenmanager.ui.theme.ScreenManagerTheme
import com.example.screenmanager.model.AppUsageSummary
import com.example.screenmanager.model.DayUiModel

@Composable
fun UsageStatsHomeScreen(
    selectedRange: UsageRange,
    selectedDayStart: Long, // NOVO
    selectedDestination: MainDestination,
    appsUsage: List<AppUsageSummary>, // NOVO
    days: List<DayUiModel>, // NOVO
    chartTitle: String, // NOVO
    chartHeadlineMinutes: Int, // NOVO
    chartPoints: List<Int>, // NOVO
    chartLabels: List<String>, // NOVO
    chartAverage: Float, // NOVO
    chartBottomLabel: String, // NOVO
    onRangeSelected: (UsageRange) -> Unit,
    onDaySelected: (Long) -> Unit, // PROMENJENO
    onAppClick: (AppUsageSummary) -> Unit, // PROMENJENO
    onDestinationSelected: (MainDestination) -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val scale = (screenWidth / 360.dp).coerceIn(0.85f, 1.3f)
    val hPadding = screenWidth * 0.04f

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B0A1F), Color(0xFF1A1233), Color(0xFF0E0B22))
                )
            ),
        containerColor = Color.Transparent,
        bottomBar = {
            BottomNavBar(
                selected = selectedDestination,
                onSelected = onDestinationSelected
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = hPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp * scale)
        ) {
            item {
                AppUsageHeader(modifier = Modifier.padding(top = 16.dp * scale))
            }
            item {
                RangeSegmentedControl(
                    selected = selectedRange,
                    onSelected = onRangeSelected
                )
            }
            item {
                // AŽURIRANO DA KORISTI NOVE PARAMETRE
                DayPicker(
                    visible = selectedRange == UsageRange.Day,
                    selectedDayStart = selectedDayStart,
                    days = days,
                    onSelectedDay = onDaySelected
                )
            }
            item {
                // AŽURIRANO DA KORISTI NOVE PARAMETRE
                UsageChartCard(
                    title = chartTitle,
                    headlineMinutes = chartHeadlineMinutes,
                    points = chartPoints,
                    labels = chartLabels,
                    average = chartAverage,
                    bottomLabel = chartBottomLabel
                )
            }
            item {
                // AŽURIRANO DA KORISTI NOVE PARAMETRE
                AppListCard(
                    apps = appsUsage,
                    onAppClick = onAppClick
                )
            }
            item {
                Spacer(modifier = Modifier.height(10.dp * scale))
            }
        }
    }
}
//
//@Preview(showBackground = true)
//@Composable
//private fun DashboardPreview() {
//    ScreenManagerTheme(dynamicColor = false) {
//        UsageChartCard(
//            range = UsageRange.Day,
//            selectedDay = MockUsage.days.last()
//        )
//    }
//}