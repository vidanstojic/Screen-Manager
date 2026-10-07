package com.example.screenmanager.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Semantička paleta aplikacije.
 *
 * Ekrani nikad ne kucaju `Color(0xFF...)` direktno — čitaju `AppTheme.colors.xyz`.
 * Ista paleta se preslikava i u Material 3 [ColorScheme] ([toMaterialColorScheme]),
 * pa M3 komponente (Switch, Slider, TextField, dijalozi) dobijaju iste boje
 * bez ručnog `colors = ...` na svakom mestu.
 */
@Immutable
data class AppColors(
    /** Pozadina ekrana. */
    val background: Color,
    /** Kartice i ostale površine prvog nivoa. */
    val surface: Color,
    /** Elementi NA kartici: chip, traka progresa, podloga ikonice. */
    val surfaceRaised: Color,
    /** Tanka ivica kartica i razdelnici. */
    val outline: Color,

    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,

    /** Jedini brend akcenat: primarna dugmad, izabrano stanje, grafici. */
    val accent: Color,
    /** Prigušena podloga u tonu akcenta (izabran chip, podloga ikonice). */
    val accentSoft: Color,
    /** Tekst/ikonica preko [accent] površine. */
    val onAccent: Color,

    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color
)

val DarkAppColors = AppColors(
    background = Color(0xFF0B0B10),
    surface = Color(0xFF15151D),
    surfaceRaised = Color(0xFF20202B),
    outline = Color(0xFF2A2A38),
    textPrimary = Color(0xFFF4F3F8),
    textSecondary = Color(0xFFA5A3B8),
    textMuted = Color(0xFF6E6C82),
    accent = Color(0xFFA78BFA),
    accentSoft = Color(0xFF2A2246),
    onAccent = Color(0xFF160F2E),
    success = Color(0xFF4ADE80),
    warning = Color(0xFFFBBF24),
    danger = Color(0xFFF87171),
    info = Color(0xFF60A5FA)
)

/** Preslikava našu paletu u Material 3 šemu da ugrađene komponente prate temu. */
fun AppColors.toMaterialColorScheme(): ColorScheme = darkColorScheme(
    primary = accent,
    onPrimary = onAccent,
    primaryContainer = accentSoft,
    onPrimaryContainer = accent,
    secondary = accent,
    onSecondary = onAccent,
    secondaryContainer = accentSoft,
    onSecondaryContainer = textPrimary,
    tertiary = info,
    onTertiary = onAccent,
    background = background,
    onBackground = textPrimary,
    surface = surface,
    onSurface = textPrimary,
    surfaceVariant = surfaceRaised,
    onSurfaceVariant = textSecondary,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = background,
    surfaceContainerLow = surface,
    surfaceContainer = surface,
    surfaceContainerHigh = surfaceRaised,
    surfaceContainerHighest = surfaceRaised,
    outline = textMuted,
    outlineVariant = outline,
    error = danger,
    onError = onAccent,
    errorContainer = danger.copy(alpha = 0.16f),
    onErrorContainer = danger,
    scrim = Color.Black
)
