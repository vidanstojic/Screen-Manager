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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.screenmanager.model.MockDayUsage
import com.example.screenmanager.model.MockUsage
import com.example.screenmanager.model.UsageRange

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
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("S", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Text(
            text = "App Usage",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

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
            .background(Color.White.copy(alpha = 0.06f))
            .border(
                1.dp,
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.05f))),
                RoundedCornerShape(22.dp)
            )
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
                    .background(
                        if (active) Brush.linearGradient(listOf(Color(0xFFB13BFF), Color(0xFF6C4CE0)))
                        else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .clickable { onSelected(range) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = range.label,
                    fontWeight = FontWeight.Bold,
                    color = if (active) Color.White else Color(0xFFA9A3C4)
                )
            }
        }
    }
}

data class DayUiModel(
    val timestamp: Long,
    val shortLabel: String,
    val dateLabel: String
)

@Composable
fun DayPicker(
    visible: Boolean,
    selectedDayStart: Long,
    days: List<DayUiModel>,
    onSelectedDay: (Long) -> Unit
) {
    if (!visible) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        days.forEach { day ->
            val active = day.timestamp == selectedDayStart
            Column(
                modifier = Modifier
                    .width(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (active) Brush.linearGradient(listOf(Color(0xFFB13BFF), Color(0xFF6C4CE0)))
                        else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.06f)))
                    )
                    .border(
                        width = 1.dp,
                        color = if (active) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onSelectedDay(day.timestamp) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = day.shortLabel,
                    color = if (active) Color.White else Color(0xFFA9A3C4),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = day.dateLabel,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}