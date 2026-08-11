package com.example.screenmanager.model

import androidx.compose.ui.graphics.Color

/**
 * Presek svih UI režima pregleda potrošnje.
 *
 * Koristi ga dashboard za izbor dnevnog ili nedeljnog prikaza.
 */
enum class UsageRange(val label: String) {
    Week("Week"),
    Day("Day")
}

/**
 * Donje navigacione destinacije kroz koje korisnik prolazi iz glavnog UI-a.
 */
enum class MainDestination(
    val navLabel: String,
    val icon: String
) {
    UsageStats("Usage\nStats", "▥"),
    UsageLimits("Usage\nLimits", "◴"),
    GeneralUsage("General\nUsage", "▦"),
    GeneralSettings("General\nSettings", "□"),
    AddLimit("Add\nLimit", "+");

    companion object {
        val bottomItems = listOf(UsageStats, UsageLimits, GeneralUsage, GeneralSettings, AddLimit)
    }
}

/**
 * Pojedinačni dan sa agregiranim podacima za grafike i listu aplikacija.
 */
data class MockDayUsage(
    val shortLabel: String,
    val dateLabel: String,
    val displayLabel: String,
    val totalMinutes: Int,
    val hourlyMinutes: List<Int>
)

/**
 * Jedna stavka potrošnje aplikacije na dashboardu ili u detaljima.
 */
data class MockAppUsage(
    val name: String,
    val minutes: Int,
    val iconText: String,
    val iconColor: Color,
    val category: String
)

/**
 * Sažetak metrika koje ekran detalja prikazuje u gridu kartica.
 */
data class AppDetailStats(
    val usageMinutes: Int,
    val sessions: Int,
    val averageMinutes: Int,
    val previousAverageMinutes: Int,
    val trendLabel: String,
    val trendColor: Color,
    val limitStatus: String
)

/**
 * Agregirana potrošnja jedne aplikacije, spremna za prikaz u dashboard listi.
 *
 * Ime i ikonica se učitavaju iz PackageManager-a preko [AppIconLoader],
 * pa ovaj model čuva samo packageName kao referencu, a ne samu sliku.
 */
data class AppUsageSummary(
    val packageName: String,
    val name: String,
    val minutes: Int,
    val category: String = "App"
)