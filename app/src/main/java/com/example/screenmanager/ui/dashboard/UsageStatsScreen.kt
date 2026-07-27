package com.example.screenmanager.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.screenmanager.model.*
import com.example.screenmanager.ui.components.BottomNavBar
import com.example.screenmanager.ui.dashboard.components.*
import com.example.screenmanager.ui.theme.ScreenManagerTheme

@Composable
fun UsageStatsHomeScreen(
    selectedRange: UsageRange,
    selectedDay: MockDayUsage,
    selectedDestination: MainDestination,
    onRangeSelected: (UsageRange) -> Unit,
    onDaySelected: (MockDayUsage) -> Unit,
    onAppClick: (MockAppUsage) -> Unit,
    onDestinationSelected: (MainDestination) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF1F5F8),
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
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AppUsageHeader(modifier = Modifier.padding(top = 16.dp))
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
                    selectedDay = selectedDay,
                    onSelectedDay = onDaySelected
                )
            }
            item {
                UsageChartCard(
                    range = selectedRange,
                    selectedDay = selectedDay
                )
            }
            item {
                AppListCard(
                    apps = MockUsage.appsFor(selectedRange, selectedDay),
                    onAppClick = onAppClick
                )
            }
            item {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardPreview() {
    ScreenManagerTheme(dynamicColor = false) {
        UsageChartCard(
            range = UsageRange.Day,
            selectedDay = MockUsage.days.last()
        )
    }
}