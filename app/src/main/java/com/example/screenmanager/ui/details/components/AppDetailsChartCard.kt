package com.example.screenmanager.ui.details.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.screenmanager.model.MockAppUsage
import com.example.screenmanager.model.MockDayUsage
import com.example.screenmanager.model.MockUsage
import com.example.screenmanager.model.UsageRange
import com.example.screenmanager.ui.theme.DetailBackground
import com.example.screenmanager.ui.theme.DetailCard
import com.example.screenmanager.ui.theme.PurpleAccent

/**
 * Grafička kartica za detalje jedne aplikacije.
 *
 * Menja prikaz između satnog i dnevnog toka i prikazuje ukupnu potrošnju.
 */
@Composable
fun AppDetailsChartCard(
    app: MockAppUsage,
    selectedRange: UsageRange,
    selectedDay: MockDayUsage,
    onToggleRange: () -> Unit
) {
    val points = if (selectedRange == UsageRange.Day) {
        MockUsage.hourlyForApp(app, selectedDay)
    } else {
        MockUsage.weeklyForApp(app)
    }
    val labels = if (selectedRange == UsageRange.Day) {
        listOf("12am", "Noon", "3pm")
    } else {
        MockUsage.days.map { it.shortLabel }
    }
    val total = points.sum()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleRange),
        shape = RoundedCornerShape(9.dp),
        colors = CardDefaults.cardColors(containerColor = DetailCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (selectedRange == UsageRange.Day) "Usage by Hour" else "Usage by Day",
                    color = Color(0xFFD8D2E0),
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(6.dp))
                        .background(PurpleAccent)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(if (selectedRange == UsageRange.Day) "▥⌁" else "⌁▥", color = DetailBackground, fontWeight = FontWeight.Bold)
                }
            }
            DarkUsageChart(
                points = points,
                labels = labels,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2.15f)
                    .padding(top = 8.dp)
            )
            DetailLegend(app = app, totalMinutes = total)
            Text(
                text = "Total Usage: ${total}m",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 14.dp)
            )
        }
    }
}

/**
 * Tamni grafikon koji crta trend potrošnje za detalje aplikacije.
 */
@Composable
private fun DarkUsageChart(
    points: List<Int>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val left = 16.dp.toPx()
        val right = 8.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = 22.dp.toPx()
        val chartWidth = size.width - left - right
        val chartHeight = size.height - top - bottom
        val maxValue = (points.maxOrNull() ?: 1).coerceAtLeast(10)

        fun xFor(index: Int): Float {
            if (points.size <= 1) return left
            return left + chartWidth * (index.toFloat() / points.lastIndex.toFloat())
        }

        fun yFor(value: Int): Float {
            val normalized = value / maxValue.toFloat()
            return top + chartHeight - chartHeight * normalized.coerceIn(0f, 1f)
        }

        for (i in 0..2) {
            val x = left + chartWidth * (i / 2f)
            drawLine(Color(0xFF6E6877), Offset(x, top), Offset(x, top + chartHeight), 1.dp.toPx())
        }
        drawLine(Color(0xFF6E6877), Offset(left, top + chartHeight), Offset(size.width - right, top + chartHeight), 1.dp.toPx())

        labels.forEachIndexed { index, label ->
            val x = left + chartWidth * (index.toFloat() / labels.lastIndex.coerceAtLeast(1).toFloat())
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x - 11.dp.toPx(),
                size.height - 5.dp.toPx(),
                android.graphics.Paint().apply {
                    color = android.graphics.Color.rgb(199, 193, 210)
                    textSize = 10.sp.toPx()
                    isAntiAlias = true
                }
            )
        }

        val linePath = Path()
        points.forEachIndexed { index, value ->
            val point = Offset(xFor(index), yFor(value))
            if (index == 0) linePath.moveTo(point.x, point.y) else linePath.lineTo(point.x, point.y)
        }
        drawPath(linePath, PurpleAccent, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))
        points.forEachIndexed { index, value ->
            drawCircle(PurpleAccent, 3.dp.toPx(), Offset(xFor(index), yFor(value)))
        }
    }
}

/**
 * Legenda sa kategorijama potrošnje unutar detalja aplikacije.
 */
@Composable
private fun DetailLegend(app: MockAppUsage, totalMinutes: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        LegendItem(color = PurpleAccent, label = "Mobile App", value = "${totalMinutes}m")
        LegendItem(color = Color(0xFF9EC8FF), label = "Mobile Web", value = "0s")
        LegendItem(color = Color(0xFFCC5D77), label = "Desktop App", value = "0s")
        LegendItem(color = app.iconColor, label = "Other", value = "0s")
    }
}

/**
 * Jedan element legende za grafikone detalja.
 */
@Composable
private fun LegendItem(color: Color, label: String, value: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(start = 7.dp))
        }
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 18.dp))
    }
}