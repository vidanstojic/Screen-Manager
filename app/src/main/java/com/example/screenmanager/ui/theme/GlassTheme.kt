package com.example.screenmanager.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Semantička paleta boja za "glass" stil aplikacije.
 *
 * Umesto da ekrani kucaju Color(0xFF...) direktno, referenciraju
 * GlassTheme.colors.xyz - tako se cela tema menja na jednom mestu
 * (npr. dark -> light) bez diranja pojedinačnih ekrana.
 */
data class GlassColorScheme(
    // Pozadina (koristi se u Brush.verticalGradient na svakom ekranu)
    val backgroundGradient: List<Color>,

    // Glass površine (kartice, chip-ovi, kontejneri)
    val surface: Color,
    val surfaceElevated: Color,
    val borderStart: Color,
    val borderEnd: Color,

    // Tekst
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,

    // Brand / akcentni gradient (primarna dugmad, aktivni tabovi, chart linije)
    val accentPrimary: Color,
    val accentSecondary: Color,

    // Semantičke boje statusa
    val success: Color,
    val warning: Color,
    val info: Color,
    val danger: Color,

    // Glow orbovi na home/hub ekranima (malo transparentniji ton akcenata)
    val glowPrimary: List<Color>,
    val glowSecondary: List<Color>,
    val glowTertiary: List<Color>
) {
    val accentGradient: Brush
        get() = Brush.linearGradient(listOf(accentPrimary, accentSecondary))

    val borderGradient: Brush
        get() = Brush.linearGradient(listOf(borderStart, borderEnd))

    val backgroundBrush: Brush
        get() = Brush.verticalGradient(backgroundGradient)
}

val DarkGlassPalette = GlassColorScheme(
    backgroundGradient = listOf(Color(0xFF0B0A1F), Color(0xFF1A1233), Color(0xFF0E0B22)),
    surface = Color.White.copy(alpha = 0.06f),
    surfaceElevated = Color.White.copy(alpha = 0.12f),
    borderStart = Color.White.copy(alpha = 0.3f),
    borderEnd = Color.White.copy(alpha = 0.05f),
    textPrimary = Color.White,
    textSecondary = Color(0xFFA9A3C4),
    textMuted = Color(0xFF6C647F),
    accentPrimary = Color(0xFFB13BFF),
    accentSecondary = Color(0xFF6C4CE0),
    success = Color(0xFF3BFFA0),
    warning = Color(0xFFFF9A5C),
    info = Color(0xFF3BC8FF),
    danger = Color(0xFFFF5CA8),
    glowPrimary = listOf(Color(0xFFB13BFF), Color(0xFF6C4CE0)),
    glowSecondary = listOf(Color(0xFF3BC8FF), Color(0xFF4C6CE0)),
    glowTertiary = listOf(Color(0xFFFF5CA8), Color(0xFFB13BFF))
)

/**
 * Svetla varijanta - i dalje "glass" (frosted kartice, blur glow orbovi),
 * samo je bazna pozadina svetla umesto tamne, a tekst/border kontrasti
 * su okrenuti. Akcentne boje su malo zasićenije da ostanu čitljive na belom.
 */
val LightGlassPalette = GlassColorScheme(
    backgroundGradient = listOf(Color(0xFFF4F1FB), Color(0xFFEDE7FB), Color(0xFFF7F5FC)),
    surface = Color.Black.copy(alpha = 0.045f),
    surfaceElevated = Color.Black.copy(alpha = 0.08f),
    borderStart = Color.Black.copy(alpha = 0.12f),
    borderEnd = Color.Black.copy(alpha = 0.02f),
    textPrimary = Color(0xFF201B33),
    textSecondary = Color(0xFF5F5A78),
    textMuted = Color(0xFF9C97B3),
    accentPrimary = Color(0xFF9333EA),
    accentSecondary = Color(0xFF5B3FD6),
    success = Color(0xFF1FA871),
    warning = Color(0xFFE07C3E),
    info = Color(0xFF2196C9),
    danger = Color(0xFFE0447A),
    glowPrimary = listOf(Color(0xFF9333EA), Color(0xFF5B3FD6)),
    glowSecondary = listOf(Color(0xFF2196C9), Color(0xFF4C6CE0)),
    glowTertiary = listOf(Color(0xFFE0447A), Color(0xFF9333EA))
)

private val LocalGlassColors = compositionLocalOf { DarkGlassPalette }

object GlassTheme {
    val colors: GlassColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassColors.current
}

@Composable
fun ScreenManagerGlassTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkGlassPalette else LightGlassPalette
    CompositionLocalProvider(LocalGlassColors provides palette) {
        content()
    }
}

/**
 * Helper - pozadina koju svaki ekran koristi umesto ručnog Brush.verticalGradient.
 */
@Composable
fun GlassBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassTheme.colors.backgroundBrush)
    ) {
        content()
    }
}