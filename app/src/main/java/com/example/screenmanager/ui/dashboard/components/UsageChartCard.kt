package com.example.screenmanager.ui.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.screenmanager.model.MockDayUsage
import com.example.screenmanager.model.MockUsage
import com.example.screenmanager.model.UsageRange
import com.example.screenmanager.utils.formatCompactMinutes
import com.example.screenmanager.utils.formatHeadline
import com.example.screenmanager.utils.formatSentenceMinutes

@Composable
fun UsageChartCard(
    range: UsageRange,
    selectedDay: MockDayUsage
) {
    val weekUsage = MockUsage.weekUsage
    val title = if (range == UsageRange.Day) {
        "${selectedDay.displayLabel}'s Usage"
    } else {
        "Last 7 Days"
    }
    val headlineMinutes = if (range == UsageRange.Day) {
        selectedDay.totalMinutes
    } else {
        weekUsage.sumOf { it.totalMinutes }
    }
    val points = if (range == UsageRange.Day) selectedDay.hourlyMinutes else weekUsage.map { it.totalMinutes }
    val labels = if (range == UsageRange.Day) {
        listOf("12am", "6am", "Noon", "6pm", "11pm")
    } else {
        weekUsage.map { it.shortLabel }
    }
    val average = if (range == UsageRange.Day) selectedDay.hourlyMinutes.average() else weekUsage.map { it.totalMinutes }.average()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, color = Color(0xFF6B7680), fontSize = 16.sp)
            Text(
                text = formatHeadline(headlineMinutes),
                color = Color(0xFF25A7F2),
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 34.sp
            )
            UsageLineChart(
                points = points,
                labels = labels,
                average = average.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.95f)
                    .padding(top = 10.dp)
            )
            Text(
                text = if (range == UsageRange.Day) {
                    "Daily Average: ${formatCompactMinutes(MockUsage.dailyAverageMinutes)}"
                } else {
                    "Average Day: ${formatCompactMinutes((headlineMinutes / 7f).toInt())}"
                },
                color = Color(0xFFF3A346),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Usage: ${formatSentenceMinutes(headlineMinutes)}",
                    color = Color(0xFF7D8992),
                    fontSize = 14.sp
                )
                Text(
                    text = "More  >",
                    color = Color(0xFF2EA7F0),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun UsageLineChart(
    points: List<Int>,
    labels: List<String>,
    average: Float,
    modifier: Modifier = Modifier
) {
    val blue = Color(0xFF25A7F2)
    val orange = Color(0xFFF1A64B)
    Canvas(modifier = modifier) {
        val left = 22.dp.toPx()
        val right = 8.dp.toPx()
        val top = 10.dp.toPx()
        val bottom = 24.dp.toPx()
        val chartWidth = size.width - left - right
        val chartHeight = size.height - top - bottom
        val maxValue = (points.maxOrNull() ?: 1).coerceAtLeast(60)
        val minValue = 0

        fun xFor(index: Int): Float {
            if (points.size <= 1) return left
            return left + chartWidth * (index.toFloat() / (points.lastIndex).toFloat())
        }

        fun yFor(value: Float): Float {
            val normalized = (value - minValue) / (maxValue - minValue).toFloat()
            return top + chartHeight - chartHeight * normalized.coerceIn(0f, 1f)
        }

        for (i in 0..4) {
            val y = top + chartHeight * (i / 4f)
            drawLine(
                color = Color(0xFFE9EEF2),
                start = Offset(left, y),
                end = Offset(size.width - right, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        labels.forEachIndexed { index, label ->
            val x = left + chartWidth * (index.toFloat() / (labels.lastIndex).coerceAtLeast(1).toFloat())
            drawLine(
                color = Color(0xFFE9EEF2),
                start = Offset(x, top),
                end = Offset(x, top + chartHeight),
                strokeWidth = 1.dp.toPx()
            )
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x - 12.dp.toPx(),
                size.height - 5.dp.toPx(),
                android.graphics.Paint().apply {
                    color = android.graphics.Color.rgb(118, 130, 140)
                    textSize = 10.sp.toPx()
                    isAntiAlias = true
                }
            )
        }

        val averageY = yFor(average)
        drawLine(
            color = orange,
            start = Offset(left, averageY),
            end = Offset(size.width - right, averageY),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
        )

        val linePath = Path()
        val fillPath = Path()
        points.forEachIndexed { index, value ->
            val point = Offset(xFor(index), yFor(value.toFloat()))
            if (index == 0) {
                linePath.moveTo(point.x, point.y)
                fillPath.moveTo(point.x, top + chartHeight)
                fillPath.lineTo(point.x, point.y)
            } else {
                linePath.lineTo(point.x, point.y)
                fillPath.lineTo(point.x, point.y)
            }
        }
        fillPath.lineTo(xFor(points.lastIndex), top + chartHeight)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x5525A7F2), Color(0x0525A7F2)),
                startY = top,
                endY = top + chartHeight
            )
        )
        drawPath(
            path = linePath,
            color = blue,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
        points.forEachIndexed { index, value ->
            val point = Offset(xFor(index), yFor(value.toFloat()))
            drawCircle(Color.White, radius = 5.dp.toPx(), center = point)
            drawCircle(
                color = if (index == points.lastIndex) Color(0xFFFF625A) else blue,
                radius = 4.dp.toPx(),
                center = point
            )
        }
    }
}