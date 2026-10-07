package com.example.screenmanager.ui.feature.limits

import com.example.screenmanager.model.AppLimitRule
import com.example.screenmanager.model.ScheduleRule
import com.example.screenmanager.model.SessionLimitRule
import com.example.screenmanager.model.ShortVideoConfig
import com.example.screenmanager.model.ShortsMode
import com.example.screenmanager.model.WakeUpConfig
import com.example.screenmanager.ui.common.formatClock
import com.example.screenmanager.ui.common.formatMinutes
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Kratki opisi pravila u jednom redu ("1h per day · Instagram, YouTube").
 * Koriste ih lista pravila (Limits), Overview i ekran detalja aplikacije, pa
 * je isto pravilo svuda opisano istim rečima.
 */

/** Aplikacije koje detektor Shorts/Reels sadržaja podržava. */
object ShortsApps {
    const val YOUTUBE = "com.google.android.youtube"
    const val INSTAGRAM = "com.instagram.android"
    val supported = listOf(YOUTUBE, INSTAGRAM)
}

val ShortsMode.label: String
    get() = when (this) {
        ShortsMode.BLOCKED -> "Block"
        ShortsMode.BUDGET -> "Daily budget"
        ShortsMode.SESSIONS -> "Sessions"
    }

fun AppLimitRule.summary(labelFor: (String) -> String): String =
    "${formatMinutes(dailyLimitMinutes)} per day · ${appsSummary(selectedAppIds, labelFor)}"

fun SessionLimitRule.summary(labelFor: (String) -> String): String {
    val sessions = sessionsSummary(maxSessions, sessionLengthMinutes, cooldownMinutes)
    return "$sessions · ${appsSummary(selectedAppIds, labelFor)}"
}

fun ScheduleRule.summary(labelFor: (String) -> String): String {
    val hours = "${formatClock(startTime)}–${formatClock(endTime)}"
    return "$hours · ${daysSummary(daysOfWeek)} · ${appsSummary(selectedAppIds, labelFor)}"
}

fun ShortVideoConfig.summary(): String = when (mode) {
    ShortsMode.BLOCKED -> "Always blocked"
    ShortsMode.BUDGET ->
        "${formatMinutes(maxReelsWatchMinutes)} per day, then the app is blocked for ${formatMinutes(fullAppBlockMinutes)}"
    ShortsMode.SESSIONS -> sessionsSummary(maxSessions, sessionLengthMinutes, cooldownMinutes)
}

fun WakeUpConfig.summary(labelFor: (String) -> String): String =
    "${formatMinutes(blockDurationMinutes)} after waking up · ${appsSummary(selectedAppIds, labelFor)}"

private fun sessionsSummary(maxSessions: Int, lengthMinutes: Int, cooldownMinutes: Int): String {
    val base = "$maxSessions × ${formatMinutes(lengthMinutes)}"
    return if (cooldownMinutes > 0) "$base, ${formatMinutes(cooldownMinutes)} break" else base
}

/** "Instagram, YouTube" ili "Instagram, YouTube +2". */
fun appsSummary(packageNames: List<String>, labelFor: (String) -> String): String = when {
    packageNames.isEmpty() -> "No apps"
    packageNames.size <= 2 -> packageNames.joinToString { labelFor(it) }
    else -> packageNames.take(2).joinToString { labelFor(it) } + " +${packageNames.size - 2}"
}

/** "Every day", "Weekdays", "Weekends" ili "Mon, Wed, Fri". */
fun daysSummary(days: Set<DayOfWeek>): String {
    val weekdays = setOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    )
    val weekend = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    return when {
        days.isEmpty() -> "No days"
        days.size == 7 -> "Every day"
        days == weekdays -> "Weekdays"
        days == weekend -> "Weekends"
        else -> days.sorted().joinToString { it.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) }
    }
}
