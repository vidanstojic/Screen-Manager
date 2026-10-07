package com.example.screenmanager.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Semantička paleta aplikacije — stil "aurora / glass": tamna pozadina sa
 * mekim mrljama boje, poluprovidne "staklene" površine, gradijent akcenat.
 *
 * Ekrani nikad ne kucaju `Color(0xFF...)` direktno — čitaju `AppTheme.colors.xyz`.
 * Ista paleta se preslikava i u Material 3 [ColorScheme] ([toMaterialColorScheme]),
 * pa M3 komponente (Switch, Slider, TextField, dijalozi) dobijaju iste boje.
 */
@Immutable
data class AppColors(
    /** Osnovna boja pozadine (ispod aurora mrlja). */
    val background: Color,
    /** Boje mrlja koje [AuroraBackground] crta preko pozadine. */
    val auroraPrimary: Color,
    val auroraSecondary: Color,
    val auroraTertiary: Color,

    /** NEPROVIDNA površina — dijalozi i donji listovi (sadržaj ispod njih ne sme da se vidi). */
    val surface: Color,
    /** Neprovidna površina višeg nivoa (meniji, elementi u dijalozima). */
    val surfaceHigh: Color,
    /** Poluprovidna podloga elemenata NA kartici: chip, traka progresa, okruglo dugme. */
    val surfaceRaised: Color,
    /** Tanke linije: razdelnici, mreža grafika. */
    val outline: Color,

    /** "Staklo" kartice: ispuna ide od [glassTop] ka [glassBottom], ivica od [glassBorderTop] ka [glassBorderBottom]. */
    val glassTop: Color,
    val glassBottom: Color,
    val glassBorderTop: Color,
    val glassBorderBottom: Color,

    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,

    /** Svetli akcenat: tekst-linkovi, ikonice, izabrano stanje sitnih elemenata. */
    val accent: Color,
    /** Početak i kraj akcentnog gradijenta (primarna dugmad, izabran tab/dan, grafici). */
    val accentStart: Color,
    val accentEnd: Color,
    /** Prigušena podloga u tonu akcenta. */
    val accentSoft: Color,
    /** Tekst/ikonica preko akcentnog gradijenta. */
    val onAccent: Color,
    /** Isticanje jedne stavke unutar akcentnog sadržaja (izabran stubić grafika). */
    val highlight: Color,

    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color
) {
    /** Ispuna staklene površine. */
    val glassBrush: Brush
        get() = Brush.linearGradient(listOf(glassTop, glassBottom))

    /** Ivica staklene površine: svetlija gore-levo, gubi se dole-desno. */
    val glassBorderBrush: Brush
        get() = Brush.linearGradient(listOf(glassBorderTop, glassBorderBottom))

    /** Akcentni gradijent za vodoravne/dijagonalne površine (dugmad, izabrana stavka). */
    val accentBrush: Brush
        get() = Brush.linearGradient(listOf(accentStart, accentEnd))

    /** Akcentni gradijent odozgo nadole (stubići grafika). */
    val accentVerticalBrush: Brush
        get() = Brush.verticalGradient(listOf(accent, accentEnd))
}

val DarkAppColors = AppColors(
    background = Color(0xFF07070F),
    auroraPrimary = Color(0xFF7C3AED),
    auroraSecondary = Color(0xFF0EA5E9),
    auroraTertiary = Color(0xFFDB2777),
    surface = Color(0xFF15142A),
    surfaceHigh = Color(0xFF211F3D),
    surfaceRaised = Color.White.copy(alpha = 0.09f),
    outline = Color.White.copy(alpha = 0.09f),
    glassTop = Color.White.copy(alpha = 0.11f),
    glassBottom = Color.White.copy(alpha = 0.035f),
    glassBorderTop = Color.White.copy(alpha = 0.22f),
    glassBorderBottom = Color.White.copy(alpha = 0.05f),
    textPrimary = Color(0xFFF5F3FF),
    textSecondary = Color(0xFFB9B4D6),
    textMuted = Color(0xFF7C7799),
    accent = Color(0xFFA78BFA),
    accentStart = Color(0xFF8B5CF6),
    accentEnd = Color(0xFF6366F1),
    accentSoft = Color(0xFF2B2452),
    onAccent = Color.White,
    highlight = Color(0xFF67E8F9),
    success = Color(0xFF6EE7B7),
    warning = Color(0xFFFBBF24),
    danger = Color(0xFFFB7185),
    info = Color(0xFF67E8F9)
)

/** Tamna boja teksta preko SVETLOG akcenta ([AppColors.accent]) u M3 komponentama. */
private val OnLightAccent = Color(0xFF1A1238)

/**
 * Preslikava našu paletu u Material 3 šemu da ugrađene komponente prate temu.
 * M3 kontejneri (dijalog, donji list, meni) koriste NEPROVIDNE površine.
 */
fun AppColors.toMaterialColorScheme(): ColorScheme = darkColorScheme(
    primary = accent,
    onPrimary = OnLightAccent,
    primaryContainer = accentSoft,
    onPrimaryContainer = accent,
    secondary = accent,
    onSecondary = OnLightAccent,
    secondaryContainer = accentSoft,
    onSecondaryContainer = textPrimary,
    tertiary = info,
    onTertiary = OnLightAccent,
    background = background,
    onBackground = textPrimary,
    surface = surface,
    onSurface = textPrimary,
    surfaceVariant = surfaceHigh,
    onSurfaceVariant = textSecondary,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = background,
    surfaceContainerLow = surface,
    surfaceContainer = surface,
    surfaceContainerHigh = surfaceHigh,
    surfaceContainerHighest = surfaceHigh,
    outline = textMuted,
    outlineVariant = outline,
    error = danger,
    onError = OnLightAccent,
    errorContainer = danger.copy(alpha = 0.16f),
    onErrorContainer = danger,
    scrim = Color.Black
)
