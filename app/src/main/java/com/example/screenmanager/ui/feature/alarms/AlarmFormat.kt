package com.example.screenmanager.ui.feature.alarms

import com.example.screenmanager.model.AlarmRule
import com.example.screenmanager.ui.feature.limits.daysSummary
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * [AlarmRule.repeatDays] čuva dane kao tekst ("Mon", "Tue"...). Ovde je
 * jedino mesto gde se taj zapis prevodi u [DayOfWeek] i nazad.
 */
private fun DayOfWeek.token(): String = getDisplayName(TextStyle.SHORT, Locale.ENGLISH)

fun Set<String>.toDaysOfWeek(): Set<DayOfWeek> =
    DayOfWeek.entries.filter { it.token() in this }.toSet()

fun Set<DayOfWeek>.toRepeatTokens(): Set<String> = map { it.token() }.toSet()

/** "Once", "Every day", "Weekdays", "Mon, Wed"... */
fun AlarmRule.repeatSummary(): String {
    val days = repeatDays.toDaysOfWeek()
    return if (days.isEmpty()) "Once" else daysSummary(days)
}

/** "in 7h 20m", "in 2d 3h", "in less than a minute". */
fun formatTimeUntil(remainingMs: Long): String {
    val totalMinutes = (remainingMs / 60_000L).coerceAtLeast(0)
    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes % (24 * 60)) / 60
    val minutes = totalMinutes % 60
    return when {
        days > 0 -> "in ${days}d ${hours}h"
        hours > 0 -> "in ${hours}h ${minutes}m"
        minutes > 0 -> "in ${minutes}m"
        else -> "in less than a minute"
    }
}
