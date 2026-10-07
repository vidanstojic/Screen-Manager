package com.example.screenmanager.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Pozadina cele aplikacije: tamna osnova sa tri meke mrlje boje ("aurora").
 *
 * Crta se JEDNOM, u korenu UI-a ([com.example.screenmanager.ui.navigation.AppNavHost]);
 * ekrani su providni i klize preko nje. Posebno je koriste samo prozori koji
 * nisu deo tog stabla: dijalog preko celog ekrana i blok ekran.
 *
 * Mrlje su radijalni gradijenti koji se gube u providno — izgledaju kao
 * zamućeni krugovi, a ne koštaju ništa (nema blur efekta).
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .drawBehind {
                val w = size.width
                val h = size.height
                // gore desno, sredina levo, dole desno
                orb(colors.auroraPrimary, alpha = 0.60f, center = Offset(w * 1.02f, h * 0.03f), radius = w * 0.95f)
                orb(colors.auroraSecondary, alpha = 0.30f, center = Offset(-w * 0.12f, h * 0.46f), radius = w * 0.78f)
                orb(colors.auroraTertiary, alpha = 0.30f, center = Offset(w * 1.08f, h * 0.86f), radius = w * 0.85f)
            },
        content = content
    )
}

private fun DrawScope.orb(color: Color, alpha: Float, center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to color.copy(alpha = alpha),
                0.45f to color.copy(alpha = alpha * 0.5f),
                1f to Color.Transparent
            ),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}
