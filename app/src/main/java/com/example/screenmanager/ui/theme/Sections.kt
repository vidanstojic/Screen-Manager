package com.example.screenmanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * Delovi aplikacije. Svaki ima svoju boju, da se odmah vidi gde si:
 * Screen Manager je ljubičast, Smart Alarms topao (boje izlaska sunca),
 * početni ekran meša oba.
 */
enum class AppSection { Launcher, ScreenManager, Alarms }

/** Paleta za [section]: menjaju se samo boje pozadine i akcenta, ostalo je zajedničko. */
fun AppColors.forSection(section: AppSection): AppColors = when (section) {
    AppSection.ScreenManager -> this

    AppSection.Launcher -> copy(
        auroraTertiary = Color(0xFFD97706)
    )

    AppSection.Alarms -> copy(
        auroraPrimary = Color(0xFFD97706),
        auroraSecondary = Color(0xFFE11D48),
        auroraTertiary = Color(0xFFEA580C),
        accent = Color(0xFFFDBA74),
        accentStart = Color(0xFFF97316),
        accentEnd = Color(0xFFE11D48),
        accentSoft = Color(0xFF3B2216),
        highlight = Color(0xFFFDE68A)
    )
}

/**
 * Primenjuje boje dela aplikacije na [content] (i na Material 3 komponente u njemu).
 * Boje se računaju od osnovne palete, ne od već promenjene, pa se teme ne "slažu".
 */
@Composable
fun SectionTheme(section: AppSection, content: @Composable () -> Unit) {
    val colors = DarkAppColors.forSection(section)
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes,
            content = content
        )
    }
}
