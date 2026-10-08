package com.example.screenmanager.ui.feature.alarms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

/**
 * Boje doba dana za alarm u [hour] časova: izlazak sunca (topla), dan (plava),
 * zalazak (narandžasto-ljubičasta), noć (indigo). Prva boja je svetlija.
 */
fun timeOfDayColors(hour: Int): List<Color> = when (hour) {
    in 5..10 -> listOf(Color(0xFFFCD34D), Color(0xFFF43F5E))
    in 11..16 -> listOf(Color(0xFF7DD3FC), Color(0xFF22D3EE))
    in 17..20 -> listOf(Color(0xFFFDBA74), Color(0xFFA855F7))
    else -> listOf(Color(0xFFA5B4FC), Color(0xFF4F46E5))
}

/** Ikonica doba dana (sunce, oblak/dan, zalazak, mesec). */
fun timeOfDayIcon(hour: Int): ImageVector = when (hour) {
    in 5..10 -> Icons.Rounded.WbTwilight
    in 11..16 -> Icons.Rounded.WbSunny
    in 17..20 -> Icons.Rounded.WbTwilight
    else -> Icons.Rounded.Bedtime
}
