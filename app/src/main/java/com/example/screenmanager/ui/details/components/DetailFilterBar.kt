package com.example.screenmanager.ui.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.screenmanager.model.DayUiModel
import com.example.screenmanager.model.UsageRange
import com.example.screenmanager.ui.theme.GlassTheme

@Composable
fun DetailFilterBar(
    selectedRange: UsageRange,
    selectedDay: DayUiModel,
    days: List<DayUiModel>,
    onRangeSelected: (UsageRange) -> Unit,
    onDaySelected: (DayUiModel) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Reset Filters", color = GlassTheme.colors.textSecondary, fontWeight = FontWeight.SemiBold)
            Text("×", color = GlassTheme.colors.textSecondary, fontSize = 22.sp)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DarkPill("<")
            UsageRange.entries.reversed().forEach { range ->
                DarkPill(
                    text = if (range == UsageRange.Day) selectedDay.displayLabel else "Last 7 Days",
                    active = selectedRange == range,
                    onClick = { onRangeSelected(range) }
                )
            }
            DarkPill("All Devices")
            DarkPill("Usage Time")
        }
        if (selectedRange == UsageRange.Day) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                days.forEach { day ->
                    DarkPill(
                        text = day.shortLabel,
                        active = selectedDay.timestamp == day.timestamp,
                        onClick = { onDaySelected(day) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DarkPill(text: String, active: Boolean = false, onClick: () -> Unit = {}) {
    val chipShape = RoundedCornerShape(10.dp)
    val backgroundModifier = if (active) {
        Modifier.background(GlassTheme.colors.accentGradient)
    } else {
        Modifier.background(GlassTheme.colors.surface)
    }

    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(chipShape)
            .then(backgroundModifier)
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        GlassTheme.colors.borderStart.copy(alpha = if (active) 0.6f else 0.3f),
                        GlassTheme.colors.borderEnd
                    )
                ),
                chipShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = GlassTheme.colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}