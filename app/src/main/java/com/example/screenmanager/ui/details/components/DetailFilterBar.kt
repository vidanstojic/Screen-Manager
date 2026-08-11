package com.example.screenmanager.ui.details.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.screenmanager.model.MockDayUsage
import com.example.screenmanager.model.MockUsage
import com.example.screenmanager.model.UsageRange
import com.example.screenmanager.ui.theme.DetailCard
import com.example.screenmanager.ui.theme.PurpleAccent

/**
 * Filter traka za detalje aplikacije.
 *
 * Pokriva izbor opsega, dana i dodatnih lokalnih filtera koji menjaju grafikone
 * i metrike u [AppDetailsScreen].
 */
@Composable
fun DetailFilterBar(
    selectedRange: UsageRange,
    selectedDay: MockDayUsage,
    onRangeSelected: (UsageRange) -> Unit,
    onDaySelected: (MockDayUsage) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Reset Filters", color = Color(0xFFCFC9DA), fontWeight = FontWeight.SemiBold)
            Text("×", color = Color(0xFFCFC9DA), fontSize = 22.sp)
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
                MockUsage.days.forEach { day ->
                    DarkPill(
                        text = day.shortLabel,
                        active = selectedDay == day,
                        onClick = { onDaySelected(day) }
                    )
                }
            }
        }
    }
}

/**
 * Tamni pill kontroler koji predstavlja jedan aktivni ili neaktivni filter.
 */
@Composable
private fun DarkPill(text: String, active: Boolean = false, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) Color(0xFF5A5367) else DetailCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (active) PurpleAccent else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}