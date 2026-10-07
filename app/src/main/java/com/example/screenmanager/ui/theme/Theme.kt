package com.example.screenmanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

/**
 * Pristup temi iz composable-a:
 * - boje:        `AppTheme.colors.accent`
 * - tipografija: `MaterialTheme.typography.titleMedium`
 * - oblici:      `MaterialTheme.shapes.large`
 * - razmaci:     [Spacing]
 */
object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

/**
 * Jedina tema aplikacije (tamna). Koriste je i [com.example.screenmanager.MainActivity]
 * i blok ekran koji se crta preko drugih aplikacija.
 *
 * Svetla varijanta se dodaje tako što se napravi druga [AppColors] paleta i
 * ovde izabere — ekrani se ne menjaju.
 */
@Composable
fun ScreenManagerTheme(content: @Composable () -> Unit) {
    val colors = DarkAppColors
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
