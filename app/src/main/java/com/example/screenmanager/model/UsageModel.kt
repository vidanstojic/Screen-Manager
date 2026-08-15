package com.example.screenmanager.model

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import kotlinx.parcelize.Parcelize

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
@Parcelize
enum class MainDestination(
    val navLabel: String,
    val icon: String
) : Parcelable {
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
@Parcelize
data class AppUsageSummary(
    val packageName: String,
    val name: String,
    val minutes: Int,
    val category: String = "App"
) : Parcelable