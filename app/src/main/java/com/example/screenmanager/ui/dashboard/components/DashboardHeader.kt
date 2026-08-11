package com.example.screenmanager.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.screenmanager.model.MockDayUsage
import com.example.screenmanager.model.MockUsage
import com.example.screenmanager.model.UsageRange

/**
 * Gornji deo dashboard pregleda sa naslovom, vremenskim opsegom i izborom dana.
 */
@Composable
fun AppUsageHeader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFFEAF1F5))
                .border(1.dp, Color(0xFFB8C6CF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("S", color = Color(0xFF687884), fontWeight = FontWeight.Bold)
        }
        Text(
            text = "App Usage",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF68727A)
        )
    }
}

/**
 * Segmentirani prekidač za izbor dnevnog ili nedeljnog prikaza potrošnje.
 */
@Composable
fun RangeSegmentedControl(
    selected: UsageRange,
    onSelected: (UsageRange) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .padding(horizontal = 34.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFFDDE8EF))
            .border(1.dp, Color(0xFFC8D5DD), RoundedCornerShape(22.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        UsageRange.entries.forEach { range ->
            val active = selected == range
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (active) Color.White else Color.Transparent)
                    .clickable { onSelected(range) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = range.label,
                    fontWeight = FontWeight.Bold,
                    color = if (active) Color(0xFF269FE8) else Color(0xFF6D7882)
                )
            }
        }
    }
}

/**
 * Horizontalni izbor dana koji se prikazuje samo u dnevnom režimu.
 */
@Composable
fun DayPicker(
    visible: Boolean,
    selectedDay: MockDayUsage,
    onSelectedDay: (MockDayUsage) -> Unit
) {
    if (!visible) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MockUsage.days.forEach { day ->
            val active = day == selectedDay
            Column(
                modifier = Modifier
                    .width(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (active) Color(0xFF2EA7F0) else Color.White)
                    .border(
                        width = 1.dp,
                        color = if (active) Color(0xFF2EA7F0) else Color(0xFFDCE5EA),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onSelectedDay(day) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = day.shortLabel,
                    color = if (active) Color.White else Color(0xFF7D8992),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = day.dateLabel,
                    color = if (active) Color.White else Color(0xFF4E5B65),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}