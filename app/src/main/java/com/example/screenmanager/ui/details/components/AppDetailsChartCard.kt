package com.example.screenmanager.ui.details.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.screenmanager.model.AppUsageSummary
import com.example.screenmanager.model.UsageRange
import com.example.screenmanager.ui.theme.GlassTheme

@Composable
fun AppDetailsChartCard(
    app: AppUsageSummary,
    selectedRange: UsageRange,
    hourlyPoints: List<Int>,
    dailyPoints: List<Int>,
    dailyLabels: List<String>,
    onToggleRange: () -> Unit
) {
    val points = if (selectedRange == UsageRange.Day) hourlyPoints else dailyPoints
    val labels = if (selectedRange == UsageRange.Day) {
        listOf("12am", "Noon", "11pm")
    } else {
        dailyLabels
    }
    val total = points.sum()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GlassTheme.colors.surface)
            .border(1.dp, GlassTheme.colors.borderGradient, RoundedCornerShape(18.dp))
            .clickable(onClick = onToggleRange)
            .padding(14.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (selectedRange == UsageRange.Day) "Usage by Hour" else "Usage by Day",
                color = GlassTheme.colors.textSecondary,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(6.dp))
                    .background(GlassTheme.colors.accentGradient)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (selectedRange == UsageRange.Day) "▥⌁" else "⌁▥",
                    color = GlassTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
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
            color = GlassTheme.colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 14.dp)
        )
    }
}

@Composable
private fun DarkUsageChart(
    points: List<Int>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    val accentColor = GlassTheme.colors.accentPrimary
    val gridColor = GlassTheme.colors.borderStart.copy(alpha = 0.2f)
    val labelColorArgb = GlassTheme.colors.textSecondary.toArgb()
    val circleBgColor = GlassTheme.colors.surface

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
            drawLine(gridColor, Offset(x, top), Offset(x, top + chartHeight), 1.dp.toPx())
        }
        drawLine(
            gridColor,
            Offset(left, top + chartHeight),
            Offset(size.width - right, top + chartHeight),
            1.dp.toPx()
        )

        labels.forEachIndexed { index, label ->
            val x = left + chartWidth * (index.toFloat() / labels.lastIndex.coerceAtLeast(1).toFloat())
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x - 11.dp.toPx(),
                size.height - 5.dp.toPx(),
                android.graphics.Paint().apply {
                    color = labelColorArgb
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
        drawPath(linePath, accentColor, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))
        points.forEachIndexed { index, value ->
            drawCircle(circleBgColor, radius = 4.dp.toPx(), center = Offset(xFor(index), yFor(value)))
            drawCircle(accentColor, radius = 3.dp.toPx(), center = Offset(xFor(index), yFor(value)))
        }
    }
}

@Composable
private fun DetailLegend(app: AppUsageSummary, totalMinutes: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        LegendItem(color = GlassTheme.colors.accentPrimary, label = "Mobile App", value = "${totalMinutes}m")
        LegendItem(color = GlassTheme.colors.info, label = "Mobile Web", value = "0s")
        LegendItem(color = GlassTheme.colors.danger, label = "Desktop App", value = "0s")
        LegendItem(color = GlassTheme.colors.textMuted, label = "Other", value = "0s")
    }
}

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
            Text(label, color = GlassTheme.colors.textPrimary, fontSize = 13.sp, modifier = Modifier.padding(start = 7.dp))
        }
        Text(value, color = GlassTheme.colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 18.dp))
    }
}