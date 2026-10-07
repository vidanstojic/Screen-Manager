package com.example.screenmanager.ui.model

import com.example.screenmanager.data.local.DailyUsage
import com.example.screenmanager.data.local.HourlyUsage
import com.example.screenmanager.domain.TimeBuckets
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Modeli koje ekrani potrošnje (Overview, Stats, App details) prikazuju, i
 * mapiranja iz Room projekcija u njih. Sva trajanja su u milisekundama;
 * u tekst se pretvaraju tek pri prikazu (ui/common/Formatters.kt).
 */

/** Period koji Stats i App details ekran prikazuju. */
enum class UsageRange(val label: String) {
    Day("Day"),
    Week("Week"),
    Month("Month")
}

/** Jedna aplikacija u listi potrošnje. */
data class AppUsageItem(
    val packageName: String,
    val label: String,
    val durationMs: Long
)

/** Jedan dan u traci "poslednjih 7 dana". */
data class DayOption(
    /** Početak dana (lokalna ponoć) — ključ za upit ka repozitorijumu. */
    val dayStart: Long,
    /** "Mon", "Tue"... */
    val weekday: String,
    /** Dan u mesecu: "7". */
    val dayOfMonth: String,
    val isToday: Boolean
)

/**
 * Poslednjih 7 dana (uključujući danas), od najstarijeg ka danas.
 * Indeks u listi (0..6) se poklapa sa [DailyUsage.day] iz nedeljnog upita.
 */
fun lastSevenDays(now: Long = System.currentTimeMillis()): List<DayOption> {
    val today = TimeBuckets.epochDay(now)
    return (6 downTo 0).map { daysAgo ->
        // Preko kalendara, ne "- n * 24h": dani oko DST prelaza nemaju 24h.
        val epochDay = today - daysAgo
        val date = LocalDate.ofEpochDay(epochDay)
        DayOption(
            dayStart = TimeBuckets.startOfDay(epochDay),
            weekday = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
            dayOfMonth = date.dayOfMonth.toString(),
            isToday = daysAgo == 0
        )
    }
}

/** Sati 0..23; sati bez zapisa u bazi su 0. */
fun List<HourlyUsage>.toHourlyMs(): List<Long> {
    val byHour = associate { it.hour to it.durationMs }
    return (0..23).map { byHour[it] ?: 0L }
}

/** Dani 0..[dayCount)-1 opsega; dani bez zapisa u bazi su 0. */
fun List<DailyUsage>.toDailyMs(dayCount: Int): List<Long> {
    val byDay = associate { it.day to it.durationMs }
    return (0 until dayCount).map { byDay[it] ?: 0L }
}
