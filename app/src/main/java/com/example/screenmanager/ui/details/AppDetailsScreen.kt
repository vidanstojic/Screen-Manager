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
import com.example.screenmanager.model.*
import com.example.screenmanager.ui.components.BottomNavBar
import com.example.screenmanager.ui.details.components.*
import com.example.screenmanager.ui.theme.DetailBackground
import com.example.screenmanager.ui.theme.PurpleAccent

/**
 * Ekran detalja jedne aplikacije.
 *
 * Do njega se dolazi iz dashboard liste aplikacija i ovde korisnik vidi
 * granularne statistike, grafike i ulaz u kreiranje novog limita.
 */
@Composable
fun AppDetailsScreen(
    app: MockAppUsage,
    selectedDestination: MainDestination,
    onDestinationSelected: (MainDestination) -> Unit,
    onBack: () -> Unit,
    onAddLimit: () -> Unit
) {
    var selectedRange by remember { mutableStateOf(UsageRange.Day) }
    var selectedDay by remember { mutableStateOf(MockUsage.days.last()) }
    var chartRange by remember { mutableStateOf(UsageRange.Day) }
    val details = MockUsage.detailsFor(app, selectedRange, selectedDay)

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
                    selectedDay = selectedDay,
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