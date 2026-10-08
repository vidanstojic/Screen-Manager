package com.example.screenmanager.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.screenmanager.ui.theme.AppTheme

/**
 * Kružni progres sa gradijentom i mekim sjajem; sadržaj ([content]) stoji u sredini.
 * Koriste ga dnevni cilj (Overview) i odbrojavanje blokade (blok ekran).
 *
 * @param progress 0..1; vrednosti iznad 1 crtaju pun krug.
 * @param colors boje gradijenta duž luka (od početka ka kraju).
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 128.dp,
    strokeWidth: Dp = 13.dp,
    colors: List<Color> = listOf(AppTheme.colors.accent, AppTheme.colors.highlight),
    trackColor: Color = AppTheme.colors.surfaceRaised,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val glowColor = colors.first().copy(alpha = 0.55f)
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val sweep = 360f * progress.coerceIn(0f, 1f)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke)
            )
            if (sweep <= 0f) return@Canvas

            // Sjaj: isti luk, crtan senkom ispod gradijentnog luka.
            drawIntoCanvas { canvas ->
                val paint = Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.STROKE
                    this.strokeWidth = stroke
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    color = android.graphics.Color.TRANSPARENT
                    setShadowLayer(10.dp.toPx(), 0f, 0f, glowColor.toArgb())
                }
                canvas.nativeCanvas.drawArc(
                    inset, inset, inset + arcSize.width, inset + arcSize.height,
                    -90f, sweep, false, paint
                )
            }
            // Gradijent kruži od vrha (-90°); sweepGradient počinje na 0°, pa se platno okreće.
            rotate(degrees = -90f) {
                drawArc(
                    brush = Brush.sweepGradient(colors),
                    startAngle = 0f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }
        content()
    }
}
