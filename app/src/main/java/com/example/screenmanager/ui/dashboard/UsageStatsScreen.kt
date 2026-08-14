package com.example.screenmanager.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.screenmanager.model.AppUsageSummary
import com.example.screenmanager.model.DayUiModel
import com.example.screenmanager.model.MainDestination
import com.example.screenmanager.model.UsageRange
import com.example.screenmanager.ui.dashboard.components.AppListCard
import com.example.screenmanager.ui.dashboard.components.AppUsageHeader
import com.example.screenmanager.ui.dashboard.components.DayPicker
import com.example.screenmanager.ui.dashboard.components.RangeSegmentedControl
import com.example.screenmanager.ui.dashboard.components.UsageChartCard
import com.example.screenmanager.ui.theme.GlassBackground

@Composable
fun UsageStatsHomeScreen(
    selectedRange: UsageRange,
    selectedDayStart: Long,
    selectedDestination: MainDestination,
    appsUsage: List<AppUsageSummary>,
    days: List<DayUiModel>,
    chartTitle: String,
    chartHeadlineMinutes: Int,
    chartPoints: List<Int>,
    chartLabels: List<String>,
    chartAverage: Float,
    chartBottomLabel: String,
    onRangeSelected: (UsageRange) -> Unit,
    onDaySelected: (Long) -> Unit,
    onAppClick: (AppUsageSummary) -> Unit,
    onDestinationSelected: (MainDestination) -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val scale = (screenWidth / 360.dp).coerceIn(0.85f, 1.3f)
    val hPadding = screenWidth * 0.04f

    GlassBackground {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent
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
                    DayPicker(
                        visible = selectedRange == UsageRange.Day,
                        selectedDayStart = selectedDayStart,
                        days = days,
                        onSelectedDay = onDaySelected
                    )
                }
                item {
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
}