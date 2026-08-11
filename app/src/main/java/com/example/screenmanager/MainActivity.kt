package com.example.screenmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.screenmanager.ui.ScreenManagerApp
import com.example.screenmanager.ui.theme.ScreenManagerTheme

/**
 * Ulazna Android aktivnost.
 *
 * Ovo je prvi UI korak nakon startovanja aplikacije: sistem ulazi u [onCreate],
 * aktivnost postavlja Compose temu i predaje kontrolu [ScreenManagerApp], koja
 * dalje bira početni ekran i navigaciju kroz celu aplikaciju.
 */
class MainActivity : ComponentActivity() {
    /**
     * Postavlja Compose temu i predaje kontrolu glavnom UI router-u.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScreenManagerTheme {
                ScreenManagerApp()
            }
        }
    }
}




//package com.example.screenmanager
//
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.horizontalScroll
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.aspectRatio
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.text.BasicTextField
//import androidx.compose.material3.Card
//import androidx.compose.material3.CardDefaults
//import androidx.compose.material3.FloatingActionButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Surface
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.Path
//import androidx.compose.ui.graphics.PathEffect
//import androidx.compose.ui.graphics.StrokeCap
//import androidx.compose.ui.graphics.drawscope.Stroke
//import androidx.compose.ui.graphics.nativeCanvas
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.TextStyle
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.lifecycle.ViewModelProvider
//import com.example.screenmanager.ui.dashboard.DashboardViewModel
//import com.example.screenmanager.ui.theme.ScreenManagerTheme
//import java.util.Locale
//
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        val dashboardViewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
//        setContent {
//            ScreenManagerTheme(dynamicColor = false) {
//                ScreenManagerApp(viewModel = dashboardViewModel)
//            }
//        }
//    }
//}
//
//@Composable
//private fun ScreenManagerApp(viewModel: DashboardViewModel) {
//    var selectedRange by remember { mutableStateOf(UsageRange.Day) }
//    var selectedDay by remember { mutableStateOf(MockUsage.days.last()) }
//    var selectedApp by remember { mutableStateOf<MockAppUsage?>(null) }
//    var selectedDestination by remember { mutableStateOf(MainDestination.UsageStats) }
//    var addLimitApp by remember { mutableStateOf<MockAppUsage?>(null) }
//
//    if (selectedDestination == MainDestination.AddLimit) {
//        AddLimitScreen(
//            selectedApp = addLimitApp,
//            onBack = { selectedDestination = MainDestination.UsageLimits },
//            onCancel = { selectedDestination = MainDestination.UsageLimits },
//            onSave = { selectedDestination = MainDestination.UsageLimits }
//        )
//        return
//    }
//
//    if (selectedApp != null) {
//        AppDetailsScreen(
//            app = selectedApp!!,
//            selectedDestination = selectedDestination,
//            onDestinationSelected = {
//                selectedDestination = it
//                selectedApp = null
//            },
//            onBack = { selectedApp = null },
//            onAddLimit = {
//                addLimitApp = selectedApp
//                selectedDestination = MainDestination.AddLimit
//            }
//        )
//        return
//    }
//
//    when (selectedDestination) {
//        MainDestination.UsageStats -> UsageStatsHomeScreen(
//            selectedRange = selectedRange,
//            selectedDay = selectedDay,
//            selectedDestination = selectedDestination,
//            onRangeSelected = { selectedRange = it },
//            onDaySelected = { selectedDay = it },
//            onAppClick = { selectedApp = it },
//            onDestinationSelected = { selectedDestination = it }
//        )
//
//        MainDestination.UsageLimits -> UsageLimitsScreen(
//            selectedDestination = selectedDestination,
//            onDestinationSelected = { selectedDestination = it },
//            onAddLimit = {
//                addLimitApp = null
//                selectedDestination = MainDestination.AddLimit
//            }
//        )
//
//        MainDestination.GeneralUsage -> GeneralPlaceholderScreen(
//            destination = selectedDestination,
//            title = "General Usage",
//            description = "Ovde ćemo kasnije prikazati globalne obrasce korišćenja, kategorije i dnevne rutine.",
//            onDestinationSelected = { selectedDestination = it }
//        )
//
//        MainDestination.GeneralSettings -> GeneralPlaceholderScreen(
//            destination = selectedDestination,
//            title = "General Settings",
//            description = "Ovde ćemo kasnije pomeriti opšta podešavanja aplikacije, dozvole i servisne kontrole.",
//            onDestinationSelected = { selectedDestination = it }
//        )
//
//        MainDestination.AddLimit -> Unit
//    }
//}
//@Composable
//private fun AppUsageRow(
//    app: MockAppUsage,
//    maxMinutes: Int,
//    onClick: () -> Unit
//) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable(onClick = onClick)
//            .padding(vertical = 10.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Box(
//            modifier = Modifier
//                .size(34.dp)
//                .clip(RoundedCornerShape(10.dp))
//                .background(app.iconColor)
//                .clickable(onClick = onClick),
//            contentAlignment = Alignment.Center
//        ) {
//            Text(
//                text = app.iconText,
//                color = Color.White,
//                fontWeight = FontWeight.Black,
//                fontSize = 13.sp
//            )
//        }
//        Spacer(modifier = Modifier.width(10.dp))
//        Column(modifier = Modifier.weight(1f)) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    text = app.name,
//                    color = Color(0xFF55636D),
//                    fontWeight = FontWeight.SemiBold,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis,
//                    modifier = Modifier.weight(1f)
//                )
//                Text(
//                    text = formatCompactMinutes(app.minutes),
//                    color = Color(0xFF6F7B84),
//                    fontSize = 14.sp,
//                    modifier = Modifier.padding(horizontal = 8.dp)
//                )
//                Box(
//                    modifier = Modifier
//                        .size(28.dp)
//                        .clip(CircleShape)
//                        .border(1.dp, Color(0xFF6E7B84), CircleShape),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text(">", color = Color(0xFF53616A), fontWeight = FontWeight.Bold)
//                }
//            }
//            Spacer(modifier = Modifier.height(6.dp))
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(7.dp)
//                    .clip(RoundedCornerShape(10.dp))
//                    .background(Color(0xFFDDE6E8))
//            ) {
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth((app.minutes / maxMinutes.toFloat()).coerceIn(0.04f, 1f))
//                        .height(7.dp)
//                        .clip(RoundedCornerShape(10.dp))
//                        .background(Color(0xFF169FEB))
//                )
//            }
//        }
//    }
//}
//
//@Composable
//private fun AppDetailsScreen(
//    app: MockAppUsage,
//    selectedDestination: MainDestination,
//    onDestinationSelected: (MainDestination) -> Unit,
//    onBack: () -> Unit,
//    onAddLimit: () -> Unit
//) {
//    var selectedRange by remember { mutableStateOf(UsageRange.Day) }
//    var selectedDay by remember { mutableStateOf(MockUsage.days.last()) }
//    var chartRange by remember { mutableStateOf(UsageRange.Day) }
//    val details = MockUsage.detailsFor(app, selectedRange, selectedDay)
//
//    Scaffold(
//        containerColor = DetailBackground,
//        bottomBar = {
//            BottomNavBar(
//                selected = selectedDestination,
//                onSelected = onDestinationSelected
//            )
//        },
//        floatingActionButton = {
//            FloatingActionButton(
//                onClick = onAddLimit,
//                containerColor = PurpleAccent,
//                contentColor = Color(0xFF211D2B),
//                shape = RoundedCornerShape(18.dp)
//            ) {
//                Text("+", fontSize = 26.sp, fontWeight = FontWeight.Medium)
//            }
//        }
//    ) { innerPadding ->
//        LazyColumn(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(innerPadding)
//                .padding(horizontal = 14.dp),
//            verticalArrangement = Arrangement.spacedBy(14.dp)
//        ) {
//            item {
//                AppDetailsTopBar(app = app, onBack = onBack)
//            }
//            item {
//                AppDetailsTabs()
//            }
//            item {
//                DetailFilterBar(
//                    selectedRange = selectedRange,
//                    selectedDay = selectedDay,
//                    onRangeSelected = {
//                        selectedRange = it
//                        chartRange = it
//                    },
//                    onDaySelected = { selectedDay = it }
//                )
//            }
//            item {
//                DetailStatsGrid(details = details)
//            }
//            item {
//                Text(
//                    text = "Charts",
//                    color = Color.White,
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.Bold,
//                    modifier = Modifier.padding(top = 2.dp)
//                )
//            }
//            item {
//                AppDetailsChartCard(
//                    app = app,
//                    selectedRange = chartRange,
//                    selectedDay = selectedDay,
//                    onToggleRange = {
//                        chartRange = if (chartRange == UsageRange.Day) UsageRange.Week else UsageRange.Day
//                    }
//                )
//            }
//            item {
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(bottom = 18.dp),
//                    horizontalArrangement = Arrangement.End
//                ) {
//                    Text(
//                        text = "See usage stats for ${app.category}  >",
//                        color = PurpleAccent,
//                        fontWeight = FontWeight.SemiBold,
//                        modifier = Modifier
//                            .clip(RoundedCornerShape(10.dp))
//                            .clickable { }
//                            .padding(8.dp)
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun AppDetailsTopBar(
//    app: MockAppUsage,
//    onBack: () -> Unit
//) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(top = 12.dp, bottom = 4.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Text(
//            text = "<",
//            color = Color(0xFFD7D3E4),
//            fontSize = 28.sp,
//            modifier = Modifier
//                .size(34.dp)
//                .clip(CircleShape)
//                .clickable(onClick = onBack),
//        )
//        Box(
//            modifier = Modifier
//                .size(36.dp)
//                .clip(RoundedCornerShape(10.dp))
//                .background(app.iconColor),
//            contentAlignment = Alignment.Center
//        ) {
//            Text(app.iconText, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
//        }
//        Text(
//            text = app.name,
//            color = Color.White,
//            fontSize = 20.sp,
//            fontWeight = FontWeight.SemiBold,
//            maxLines = 1,
//            overflow = TextOverflow.Ellipsis,
//            modifier = Modifier
//                .weight(1f)
//                .padding(horizontal = 10.dp)
//        )
//        Text("⋮", color = Color.White, fontSize = 28.sp)
//    }
//}
//
//@Composable
//private fun AppDetailsTabs() {
//    Row(modifier = Modifier.fillMaxWidth()) {
//        DetailTab("Stats", active = true, modifier = Modifier.weight(1f))
//        DetailTab("Settings", active = false, modifier = Modifier.weight(1f))
//    }
//}
//
//@Composable
//private fun DetailTab(
//    label: String,
//    active: Boolean,
//    modifier: Modifier = Modifier
//) {
//    Column(
//        modifier = modifier.height(44.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.SpaceBetween
//    ) {
//        Text(
//            text = label,
//            color = if (active) PurpleAccent else Color(0xFFD4CFDD),
//            fontSize = 14.sp,
//            fontWeight = FontWeight.SemiBold,
//            modifier = Modifier.padding(top = 8.dp)
//        )
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(if (active) 3.dp else 1.dp)
//                .background(if (active) PurpleAccent else Color(0xFF353140))
//        )
//    }
//}
//
//@Composable
//private fun DetailFilterBar(
//    selectedRange: UsageRange,
//    selectedDay: MockDayUsage,
//    onRangeSelected: (UsageRange) -> Unit,
//    onDaySelected: (MockDayUsage) -> Unit
//) {
//    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Text("Reset Filters", color = Color(0xFFCFC9DA), fontWeight = FontWeight.SemiBold)
//            Text("×", color = Color(0xFFCFC9DA), fontSize = 22.sp)
//        }
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .horizontalScroll(rememberScrollState()),
//            horizontalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            DarkPill("<")
//            UsageRange.entries.reversed().forEach { range ->
//                DarkPill(
//                    text = if (range == UsageRange.Day) selectedDay.displayLabel else "Last 7 Days",
//                    active = selectedRange == range,
//                    onClick = { onRangeSelected(range) }
//                )
//            }
//            DarkPill("All Devices")
//            DarkPill("Usage Time")
//        }
//        if (selectedRange == UsageRange.Day) {
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .horizontalScroll(rememberScrollState()),
//                horizontalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                MockUsage.days.forEach { day ->
//                    DarkPill(
//                        text = day.shortLabel,
//                        active = selectedDay == day,
//                        onClick = { onDaySelected(day) }
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun DarkPill(
//    text: String,
//    active: Boolean = false,
//    onClick: () -> Unit = {}
//) {
//    Box(
//        modifier = Modifier
//            .height(34.dp)
//            .clip(RoundedCornerShape(10.dp))
//            .background(if (active) Color(0xFF5A5367) else DetailCard)
//            .clickable(onClick = onClick)
//            .padding(horizontal = 12.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        Text(
//            text = text,
//            color = if (active) PurpleAccent else Color.White,
//            fontSize = 13.sp,
//            fontWeight = FontWeight.SemiBold,
//            maxLines = 1
//        )
//    }
//}
//
//@Composable
//private fun DetailStatsGrid(details: AppDetailStats) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(9.dp),
//        colors = CardDefaults.cardColors(containerColor = DetailCard)
//    ) {
//        Column {
//            Row(modifier = Modifier.fillMaxWidth()) {
//                DetailStatCell("Usage", formatDetailedDuration(details.usageMinutes), Modifier.weight(1f))
//                DetailStatCell("Sessions", details.sessions.toString(), Modifier.weight(1f), alignEnd = true)
//            }
//            Row(modifier = Modifier.fillMaxWidth()) {
//                DetailStatCell("Average in 7 Days", formatDetailedDuration(details.averageMinutes), Modifier.weight(1f))
//                DetailStatCell("Trend vs Previous Week", details.trendLabel, Modifier.weight(1f), alignEnd = true, valueColor = details.trendColor)
//            }
//            Row(modifier = Modifier.fillMaxWidth()) {
//                DetailStatCell("7 Day Baseline", formatDetailedDuration(details.previousAverageMinutes), Modifier.weight(1f), valueColor = PurpleAccent)
//                DetailStatCell("Limit Status", details.limitStatus, Modifier.weight(1f), alignEnd = true)
//            }
//        }
//    }
//}
//
//@Composable
//private fun DetailStatCell(
//    label: String,
//    value: String,
//    modifier: Modifier = Modifier,
//    alignEnd: Boolean = false,
//    valueColor: Color = Color.White
//) {
//    Column(
//        modifier = modifier
//            .height(72.dp)
//            .border(0.5.dp, Color(0xFF292632))
//            .padding(12.dp),
//        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
//        verticalArrangement = Arrangement.SpaceBetween
//    ) {
//        Text(label, color = Color(0xFFD1CBD8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
//        Text(value, color = valueColor, fontSize = 21.sp, fontWeight = FontWeight.Bold)
//    }
//}
//
//@Composable
//private fun AppDetailsChartCard(
//    app: MockAppUsage,
//    selectedRange: UsageRange,
//    selectedDay: MockDayUsage,
//    onToggleRange: () -> Unit
//) {
//    val points = if (selectedRange == UsageRange.Day) {
//        MockUsage.hourlyForApp(app, selectedDay)
//    } else {
//        MockUsage.weeklyForApp(app)
//    }
//    val labels = if (selectedRange == UsageRange.Day) {
//        listOf("12am", "Noon", "3pm")
//    } else {
//        MockUsage.days.map { it.shortLabel }
//    }
//    val total = points.sum()
//
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable(onClick = onToggleRange),
//        shape = RoundedCornerShape(9.dp),
//        colors = CardDefaults.cardColors(containerColor = DetailCard)
//    ) {
//        Column(modifier = Modifier.padding(14.dp)) {
//            Box(modifier = Modifier.fillMaxWidth()) {
//                Text(
//                    text = if (selectedRange == UsageRange.Day) "Usage by Hour" else "Usage by Day",
//                    color = Color(0xFFD8D2E0),
//                    fontWeight = FontWeight.Bold
//                )
//                Box(
//                    modifier = Modifier
//                        .align(Alignment.TopEnd)
//                        .clip(RoundedCornerShape(6.dp))
//                        .background(PurpleAccent)
//                        .padding(horizontal = 8.dp, vertical = 3.dp)
//                ) {
//                    Text(if (selectedRange == UsageRange.Day) "▥⌁" else "⌁▥", color = DetailBackground, fontWeight = FontWeight.Bold)
//                }
//            }
//            DarkUsageChart(
//                points = points,
//                labels = labels,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .aspectRatio(2.15f)
//                    .padding(top = 8.dp)
//            )
//            DetailLegend(app = app, totalMinutes = total)
//            Text(
//                text = "Total Usage: ${formatDetailedDuration(total)}",
//                color = Color.White,
//                fontSize = 15.sp,
//                fontWeight = FontWeight.Bold,
//                modifier = Modifier.padding(top = 14.dp)
//            )
//        }
//    }
//}
//
//@Composable
//private fun DarkUsageChart(
//    points: List<Int>,
//    labels: List<String>,
//    modifier: Modifier = Modifier
//) {
//    Canvas(modifier = modifier) {
//        val left = 16.dp.toPx()
//        val right = 8.dp.toPx()
//        val top = 8.dp.toPx()
//        val bottom = 22.dp.toPx()
//        val chartWidth = size.width - left - right
//        val chartHeight = size.height - top - bottom
//        val maxValue = (points.maxOrNull() ?: 1).coerceAtLeast(10)
//
//        fun xFor(index: Int): Float {
//            if (points.size <= 1) return left
//            return left + chartWidth * (index.toFloat() / points.lastIndex.toFloat())
//        }
//
//        fun yFor(value: Int): Float {
//            val normalized = value / maxValue.toFloat()
//            return top + chartHeight - chartHeight * normalized.coerceIn(0f, 1f)
//        }
//
//        for (i in 0..2) {
//            val x = left + chartWidth * (i / 2f)
//            drawLine(Color(0xFF6E6877), Offset(x, top), Offset(x, top + chartHeight), 1.dp.toPx())
//        }
//        drawLine(Color(0xFF6E6877), Offset(left, top + chartHeight), Offset(size.width - right, top + chartHeight), 1.dp.toPx())
//
//        labels.forEachIndexed { index, label ->
//            val x = left + chartWidth * (index.toFloat() / labels.lastIndex.coerceAtLeast(1).toFloat())
//            drawContext.canvas.nativeCanvas.drawText(
//                label,
//                x - 11.dp.toPx(),
//                size.height - 5.dp.toPx(),
//                android.graphics.Paint().apply {
//                    color = android.graphics.Color.rgb(199, 193, 210)
//                    textSize = 10.sp.toPx()
//                    isAntiAlias = true
//                }
//            )
//        }
//
//        val linePath = Path()
//        points.forEachIndexed { index, value ->
//            val point = Offset(xFor(index), yFor(value))
//            if (index == 0) linePath.moveTo(point.x, point.y) else linePath.lineTo(point.x, point.y)
//        }
//        drawPath(linePath, PurpleAccent, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))
//        points.forEachIndexed { index, value ->
//            drawCircle(PurpleAccent, 3.dp.toPx(), Offset(xFor(index), yFor(value)))
//        }
//    }
//}
//
//@Composable
//private fun DetailLegend(
//    app: MockAppUsage,
//    totalMinutes: Int
//) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .horizontalScroll(rememberScrollState())
//            .padding(top = 8.dp),
//        horizontalArrangement = Arrangement.spacedBy(20.dp)
//    ) {
//        LegendItem(color = PurpleAccent, label = "Mobile App", value = formatDetailedDuration(totalMinutes))
//        LegendItem(color = Color(0xFF9EC8FF), label = "Mobile Web", value = "0s")
//        LegendItem(color = Color(0xFFCC5D77), label = "Desktop App", value = "0s")
//        LegendItem(color = app.iconColor, label = "Other", value = "0s")
//    }
//}
//
//@Composable
//private fun LegendItem(
//    color: Color,
//    label: String,
//    value: String
//) {
//    Column {
//        Row(verticalAlignment = Alignment.CenterVertically) {
//            Box(
//                modifier = Modifier
//                    .size(11.dp)
//                    .clip(CircleShape)
//                    .background(color)
//            )
//            Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(start = 7.dp))
//        }
//        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 18.dp))
//    }
//}
//
//@Composable
//private fun UsageLimitMockDialog(
//    app: MockAppUsage,
//    onClose: () -> Unit
//) {
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color(0xAA000000))
//            .clickable(onClick = onClose)
//            .padding(24.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        Surface(
//            shape = RoundedCornerShape(18.dp),
//            color = DetailCard,
//            modifier = Modifier
//                .fillMaxWidth()
//                .clickable { }
//        ) {
//            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
//                Text("Usage limit", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
//                Text(
//                    "Limit podešavanja za ${app.name} ćemo povezati sa pravom konfiguracijom u sledećem koraku.",
//                    color = Color(0xFFD8D2E0)
//                )
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(44.dp)
//                        .clip(RoundedCornerShape(12.dp))
//                        .background(PurpleAccent)
//                        .clickable(onClick = onClose),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text("OK", color = DetailBackground, fontWeight = FontWeight.Bold)
//                }
//            }
//        }
//    }
//}
//
//private enum class UsageRange(val label: String) {
//    Week("Week"),
//    Day("Day")
//}
//
//private enum class MainDestination(
//    val navLabel: String,
//    val icon: String
//) {
//    UsageStats("Usage\nStats", "▥"),
//    UsageLimits("Usage\nLimits", "◴"),
//    GeneralUsage("General\nUsage", "▦"),
//    GeneralSettings("General\nSettings", "□"),
//    AddLimit("Add\nLimit", "+");
//
//    companion object {
//        val bottomItems = listOf(UsageStats, UsageLimits, GeneralUsage, GeneralSettings)
//    }
//}
//
//private data class MockDayUsage(
//    val shortLabel: String,
//    val dateLabel: String,
//    val displayLabel: String,
//    val totalMinutes: Int,
//    val hourlyMinutes: List<Int>
//)
//
//private data class MockAppUsage(
//    val name: String,
//    val minutes: Int,
//    val iconText: String,
//    val iconColor: Color,
//    val category: String
//)
//
//private data class AppDetailStats(
//    val usageMinutes: Int,
//    val sessions: Int,
//    val averageMinutes: Int,
//    val previousAverageMinutes: Int,
//    val trendLabel: String,
//    val trendColor: Color,
//    val limitStatus: String
//)
//
//private object MockUsage {
//    val days = listOf(
//        MockDayUsage("Wed", "15", "Wednesday", 301, listOf(18, 32, 41, 35, 24, 18, 12, 9, 8, 7, 12, 30, 48, 54, 42, 20, 14, 10, 8, 11, 24, 36, 31, 17)),
//        MockDayUsage("Thu", "16", "Thursday", 287, listOf(12, 18, 20, 18, 16, 10, 8, 6, 7, 12, 20, 35, 40, 46, 38, 24, 18, 14, 10, 16, 22, 28, 30, 20)),
//        MockDayUsage("Fri", "17", "Friday", 263, listOf(10, 14, 18, 16, 12, 9, 7, 8, 10, 18, 28, 34, 32, 26, 22, 18, 14, 20, 32, 38, 28, 20, 14, 8)),
//        MockDayUsage("Sat", "18", "Saturday", 436, listOf(20, 24, 35, 42, 36, 18, 10, 8, 14, 26, 44, 60, 72, 78, 68, 44, 38, 52, 64, 70, 58, 42, 30, 22)),
//        MockDayUsage("Sun", "19", "Sunday", 344, listOf(16, 20, 24, 30, 26, 18, 10, 8, 12, 20, 32, 48, 54, 50, 42, 34, 26, 22, 30, 38, 46, 40, 28, 18)),
//        MockDayUsage("Mon", "20", "Monday", 226, listOf(8, 10, 12, 14, 10, 8, 6, 5, 8, 14, 18, 24, 30, 26, 22, 18, 12, 10, 14, 20, 28, 24, 16, 10)),
//        MockDayUsage("Tue", "21", "Today", 290, listOf(28, 18, 32, 60, 70, 16, 2, 8, 8, 8, 8, 8, 8, 8, 8, 6, 4, 28, 46, 50, 48, 56, 40, 9))
//    )
//
//    val weekUsage = days
//    val dailyAverageMinutes = 265
//
//    private val dayApps = listOf(
//        MockAppUsage("Podešavanja", 99, "P", Color(0xFF7D97A7), "System"),
//        MockAppUsage("PUBG MOBILE", 63, "PB", Color(0xFF6A5B45), "Game"),
//        MockAppUsage("Instagram", 46, "IG", Color(0xFFD13ED8), "Social"),
//        MockAppUsage("YouTube", 46, "YT", Color(0xFFE53935), "Video"),
//        MockAppUsage("Chrome", 23, "CH", Color(0xFF3F8DF6), "Browser"),
//        MockAppUsage("WhatsApp", 13, "WA", Color(0xFF1BAF5D), "Messaging")
//    )
//
//    private val weekApps = listOf(
//        MockAppUsage("Instagram", 384, "IG", Color(0xFFD13ED8), "Social"),
//        MockAppUsage("YouTube", 321, "YT", Color(0xFFE53935), "Video"),
//        MockAppUsage("PUBG MOBILE", 282, "PB", Color(0xFF6A5B45), "Game"),
//        MockAppUsage("Chrome", 176, "CH", Color(0xFF3F8DF6), "Browser"),
//        MockAppUsage("Podešavanja", 126, "P", Color(0xFF7D97A7), "System"),
//        MockAppUsage("WhatsApp", 93, "WA", Color(0xFF1BAF5D), "Messaging")
//    )
//
//    fun appsFor(range: UsageRange, selectedDay: MockDayUsage): List<MockAppUsage> {
//        if (range == UsageRange.Week) return weekApps
//        val offset = days.indexOf(selectedDay).coerceAtLeast(0)
//        return dayApps.mapIndexed { index, app ->
//            val adjusted = (app.minutes * (0.72f + ((offset + index) % 5) * 0.09f)).toInt().coerceAtLeast(4)
//            app.copy(minutes = adjusted)
//        }.sortedByDescending { it.minutes }
//    }
//
//    fun detailsFor(
//        app: MockAppUsage,
//        range: UsageRange,
//        selectedDay: MockDayUsage
//    ): AppDetailStats {
//        val weekly = weeklyForApp(app)
//        val usage = if (range == UsageRange.Week) weekly.sum() else hourlyForApp(app, selectedDay).sum()
//        val sessions = if (range == UsageRange.Week) {
//            weekly.sumOf { (it / 18).coerceAtLeast(1) }
//        } else {
//            (usage / 16).coerceAtLeast(1)
//        }
//        val average = (weekly.sum() / 7f).toInt()
//        val previousAverage = (average * previousWeekFactor(app)).toInt().coerceAtLeast(1)
//        val trendPercent = ((average - previousAverage) / previousAverage.toFloat() * 100).toInt()
//        val trendUp = trendPercent >= 0
//        return AppDetailStats(
//            usageMinutes = usage,
//            sessions = sessions,
//            averageMinutes = average,
//            previousAverageMinutes = previousAverage,
//            trendLabel = if (trendUp) "+$trendPercent%" else "$trendPercent%",
//            trendColor = if (trendUp) Color(0xFFFFB06A) else Color(0xFF8BE0B0),
//            limitStatus = if (app.minutes > 60) "High" else "OK"
//        )
//    }
//
//    fun hourlyForApp(app: MockAppUsage, selectedDay: MockDayUsage): List<Int> {
//        val seed = (app.name.length + days.indexOf(selectedDay).coerceAtLeast(0)).coerceAtLeast(1)
//        val desiredTotal = app.minutesFor(selectedDay)
//        val raw = selectedDay.hourlyMinutes.mapIndexed { index, value ->
//            val pulse = if ((index + seed) % 5 == 0) 1.35f else if ((index + seed) % 3 == 0) 0.72f else 0.92f
//            (value * pulse).toInt().coerceAtLeast(0)
//        }
//        val rawTotal = raw.sum().coerceAtLeast(1)
//        return raw.map { ((it / rawTotal.toFloat()) * desiredTotal).toInt().coerceAtLeast(0) }
//    }
//
//    fun weeklyForApp(app: MockAppUsage): List<Int> {
//        return days.mapIndexed { index, day ->
//            val base = app.minutesFor(day)
//            val weekendBoost = if (day.shortLabel in setOf("Sat", "Sun")) 1.18f else 0.92f
//            val appShift = 0.82f + ((app.name.length + index) % 4) * 0.12f
//            (base * weekendBoost * appShift).toInt().coerceAtLeast(4)
//        }
//    }
//
//    private fun MockAppUsage.minutesFor(day: MockDayUsage): Int {
//        val dayIndex = days.indexOf(day).coerceAtLeast(0)
//        val base = dayApps.firstOrNull { it.name == name }?.minutes ?: minutes
//        val multiplier = 0.74f + ((dayIndex + name.length) % 6) * 0.08f
//        return (base * multiplier).toInt().coerceAtLeast(3)
//    }
//
//    private fun previousWeekFactor(app: MockAppUsage): Float {
//        return when (app.category) {
//            "Game" -> 0.78f
//            "Social" -> 1.14f
//            "Video" -> 0.91f
//            "Browser" -> 1.08f
//            else -> 0.96f
//        }
//    }
//}
//
//private fun formatHeadline(minutes: Int): String {
//    val hours = minutes / 60
//    val rest = minutes % 60
//    return if (hours > 0) "$hours hours and $rest minutes" else "$rest minutes"
//}
//
//private fun formatSentenceMinutes(minutes: Int): String {
//    val hours = minutes / 60
//    val rest = minutes % 60
//    return if (hours > 0) "$hours hours and $rest minutes" else "$rest minutes"
//}
//
//private fun formatCompactMinutes(minutes: Int): String {
//    val hours = minutes / 60
//    val rest = minutes % 60
//    return when {
//        hours > 0 && rest > 0 -> String.format(Locale.US, "%dh %dm", hours, rest)
//        hours > 0 -> String.format(Locale.US, "%dh", hours)
//        else -> String.format(Locale.US, "%dm", rest)
//    }
//}
//
//private fun formatDetailedDuration(minutes: Int): String {
//    if (minutes <= 0) return "0s"
//    val hours = minutes / 60
//    val rest = minutes % 60
//    return when {
//        hours > 0 && rest > 0 -> "${hours}h  ${rest}m"
//        hours > 0 -> "${hours}h"
//        else -> "${rest}m"
//    }
//}
//
//private val DetailBackground = Color(0xFF24212E)
//private val DetailCard = Color(0xFF45414F)
//private val PurpleAccent = Color(0xFFC39BFF)
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
