package com.example.screenmanager.ui.details.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.screenmanager.model.AppDetailStats
import com.example.screenmanager.ui.theme.DetailCard
import com.example.screenmanager.ui.theme.PurpleAccent

@Composable
fun DetailStatsGrid(details: AppDetailStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(9.dp),
        colors = CardDefaults.cardColors(containerColor = DetailCard)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                DetailStatCell("Usage", "${details.usageMinutes}m", Modifier.weight(1f))
                DetailStatCell("Sessions", details.sessions.toString(), Modifier.weight(1f), alignEnd = true)
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                DetailStatCell("Average in 7 Days", "${details.averageMinutes}m", Modifier.weight(1f))
                DetailStatCell("Trend vs Previous Week", details.trendLabel, Modifier.weight(1f), alignEnd = true, valueColor = details.trendColor)
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                DetailStatCell("7 Day Baseline", "${details.previousAverageMinutes}m", Modifier.weight(1f), valueColor = PurpleAccent)
                DetailStatCell("Limit Status", details.limitStatus, Modifier.weight(1f), alignEnd = true)
            }
        }
    }
}

@Composable
private fun DetailStatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
    valueColor: Color = Color.White
) {
    Column(
        modifier = modifier
            .height(72.dp)
            .border(0.5.dp, Color(0xFF292632))
            .padding(12.dp),
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFFD1CBD8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, color = valueColor, fontSize = 21.sp, fontWeight = FontWeight.Bold)
    }
}