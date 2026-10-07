package com.example.screenmanager.ui.common

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Jedino mesto gde se trajanja i vremena pretvaraju u tekst.
 * Svi ekrani zovu ove funkcije, pa je format svuda isti ("3h 42m", "07:30").
 */

private const val MINUTE_MS = 60_000L
private val CLOCK_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Trajanje iz milisekundi: "3h 42m", "42m", "<1m", "0m". */
fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "0m"
    if (durationMs < MINUTE_MS) return "<1m"
    return formatMinutes((durationMs / MINUTE_MS).toInt())
}

/** Trajanje iz minuta: "1h 30m", "2h", "45m". */
fun formatMinutes(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours > 0 && rest > 0 -> "${hours}h ${rest}m"
        hours > 0 -> "${hours}h"
        else -> "${rest}m"
    }
}

/** Odbrojavanje: "1:05:09" za duže od sata, inače "05:09". */
fun formatCountdown(remainingMs: Long): String {
    val totalSeconds = (remainingMs / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

/** Vreme na satu: "07:30". */
fun formatClock(hour: Int, minute: Int): String = String.format(Locale.US, "%02d:%02d", hour, minute)

fun formatClock(time: LocalTime): String = CLOCK_FORMAT.format(time)

/** Trenutak (epoch ms) kao vreme na satu u lokalnoj zoni: "14:35". */
fun formatClock(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    CLOCK_FORMAT.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

/** "1 app" / "3 apps". */
fun pluralize(count: Int, singular: String, plural: String = singular + "s"): String =
    if (count == 1) "$count $singular" else "$count $plural"
