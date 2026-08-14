package com.example.screenmanager.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.screenmanager.model.DayUiModel
import com.example.screenmanager.model.UsageRange
import com.example.screenmanager.ui.theme.GlassTheme

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
                .background(GlassTheme.colors.surfaceElevated)
                .border(1.dp, GlassTheme.colors.borderEnd, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("S", color = GlassTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
        }
        Text(
            text = "App Usage",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = GlassTheme.colors.textPrimary
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
            .background(GlassTheme.colors.surfaceElevated)
            .border(
                1.dp,
                GlassTheme.colors.borderGradient,
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
                        if (active) GlassTheme.colors.accentPrimary
                        else GlassTheme.colors.surface
                    )
                    .clickable { onSelected(range) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = range.label,
                    fontWeight = FontWeight.Bold,
                    color = if (active) GlassTheme.colors.textPrimary else GlassTheme.colors.textSecondary
                )
            }
        }
    }
}

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
                        if (active) GlassTheme.colors.accentPrimary
                        else GlassTheme.colors.surface
                    )
                    .border(
                        width = 1.dp,
                        color = if (active) GlassTheme.colors.textPrimary.copy(alpha = 0.4f) else GlassTheme.colors.borderEnd,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onSelectedDay(day.timestamp) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = day.shortLabel,
                    color = if (active) GlassTheme.colors.textPrimary else GlassTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = day.dateLabel,
                    color = GlassTheme.colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}