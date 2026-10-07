package com.example.screenmanager.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.screenmanager.ui.theme.AppTheme

/** Širina desne margine u kojoj stoje oznake Y ose. */
private val AxisGutter = 34.dp

/**
 * Stubičasti grafik potrošnje (po satu, po danu u nedelji, po danu u mesecu).
 *
 * @param valuesMs vrednost svakog stubića u milisekundama.
 * @param slotLabels natpis ispod svakog stubića; `null` = bez natpisa za taj stubić
 *        (npr. kod 24 sata natpis ide samo na svaki šesti).
 * @param selectedIndex stubić koji je korisnik dodirnuo (ostali se priguše), ili null.
 * @param onSelect dodir stubića; ponovni dodir istog stubića šalje null (poništava izbor).
 * @param averageMs ako nije null, crta isprekidanu liniju proseka.
 */
@Composable
fun BarChart(
    valuesMs: List<Long>,
    slotLabels: List<String?>,
    modifier: Modifier = Modifier,
    selectedIndex: Int? = null,
    onSelect: (Int?) -> Unit = {},
    averageMs: Long? = null,
    chartHeight: Dp = 150.dp
) {
    val barTop = AppTheme.colors.accent
    val barBottom = AppTheme.colors.accentEnd
    val selectedTop = AppTheme.colors.highlight
    val emptyBarColor = AppTheme.colors.surfaceRaised
    val gridColor = AppTheme.colors.outline
    val averageColor = AppTheme.colors.textSecondary

    val axisMaxMs = niceAxisMax(valuesMs.maxOrNull() ?: 0L)
    val barCount = valuesMs.size
    // Gest živi duže od jedne rekompozicije, pa čita uvek najnovije vrednosti.
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = AxisGutter)
                    .pointerInput(barCount) {
                        detectTapGestures { offset ->
                            if (barCount == 0) return@detectTapGestures
                            val slotWidth = size.width / barCount.toFloat()
                            val index = (offset.x / slotWidth).toInt().coerceIn(0, barCount - 1)
                            currentOnSelect(if (index == currentSelectedIndex) null else index)
                        }
                    }
            ) {
                // Vodoravne linije mreže: 0, 50% i 100% ose.
                listOf(0f, 0.5f, 1f).forEach { fraction ->
                    val y = size.height * (1f - fraction)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                if (valuesMs.isEmpty()) return@Canvas
                val slotWidth = size.width / valuesMs.size
                val barWidth = (slotWidth * 0.62f).coerceAtMost(28.dp.toPx())
                val minBarHeight = 3.dp.toPx()

                valuesMs.forEachIndexed { index, value ->
                    val fraction = (value.toFloat() / axisMaxMs).coerceIn(0f, 1f)
                    val barHeight = if (value > 0) {
                        (size.height * fraction).coerceAtLeast(minBarHeight)
                    } else {
                        minBarHeight
                    }
                    val left = slotWidth * index + (slotWidth - barWidth) / 2f
                    val top = size.height - barHeight
                    val corner = CornerRadius(barWidth / 3f, barWidth / 3f)
                    if (value <= 0) {
                        drawRoundRect(emptyBarColor, Offset(left, top), Size(barWidth, barHeight), corner)
                    } else {
                        // Gradijent ide od vrha do dna SVAKOG stubića; izabrani stubić
                        // je istaknut drugom bojom, a ostali se tada priguše.
                        val isSelected = selectedIndex == index
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(if (isSelected) selectedTop else barTop, barBottom),
                                startY = top,
                                endY = size.height
                            ),
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = corner,
                            alpha = if (selectedIndex == null || isSelected) 1f else 0.35f
                        )
                    }
                }

                if (averageMs != null && averageMs > 0) {
                    val y = size.height * (1f - (averageMs.toFloat() / axisMaxMs).coerceIn(0f, 1f))
                    drawLine(
                        color = averageColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                    )
                }
            }

            // Oznake Y ose (vrh i sredina) u desnoj margini.
            AxisLabel(text = axisLabel(axisMaxMs), modifier = Modifier.align(Alignment.TopEnd))
            AxisLabel(text = axisLabel(axisMaxMs / 2), modifier = Modifier.align(Alignment.CenterEnd))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, end = AxisGutter)
        ) {
            slotLabels.forEach { label ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (label != null) {
                        // Natpis sme da bude širi od svog stubića (mesečni prikaz).
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.textMuted,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.wrapContentWidth(unbounded = true)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AxisLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = AppTheme.colors.textMuted,
        textAlign = TextAlign.End,
        maxLines = 1,
        softWrap = false,
        modifier = modifier.width(AxisGutter)
    )
}

/**
 * Zaokružuje vrh Y ose na "lepu" vrednost (15m, 30m, 1h, 2h...) da oznake
 * ose budu okrugli brojevi, a najviši stubić ne dodiruje vrh.
 */
private fun niceAxisMax(maxValueMs: Long): Long {
    val minute = 60_000L
    val steps = listOf(10, 20, 30, 60, 120, 180, 240, 360, 480, 720, 960, 1440).map { it * minute }
    return steps.firstOrNull { it >= maxValueMs } ?: steps.last()
}

/** Kratka oznaka ose: "45m", "2h", "1.5h" (da stane u usku marginu). */
private fun axisLabel(valueMs: Long): String {
    val minutes = valueMs / 60_000L
    return when {
        minutes < 60 -> "${minutes}m"
        minutes % 60 == 0L -> "${minutes / 60}h"
        else -> "${minutes / 60}.5h"
    }
}

/** Tanka traka udela u akcentnom gradijentu (npr. udeo aplikacije u ukupnom vremenu). */
@Composable
fun ProportionBar(
    fraction: Float,
    modifier: Modifier = Modifier
) {
    val trackColor = AppTheme.colors.surfaceRaised
    val startColor = AppTheme.colors.accentStart
    val endColor = AppTheme.colors.highlight
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
    ) {
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(color = trackColor, size = size, cornerRadius = radius)
        val width = size.width * fraction.coerceIn(0f, 1f)
        if (width > 0f) {
            val fillWidth = width.coerceAtLeast(size.height)
            drawRoundRect(
                // Gradijent je razvučen preko cele trake, pa duža traka stiže do svetlije boje.
                brush = Brush.horizontalGradient(listOf(startColor, endColor), startX = 0f, endX = size.width),
                size = Size(fillWidth, size.height),
                cornerRadius = radius
            )
        }
    }
}
