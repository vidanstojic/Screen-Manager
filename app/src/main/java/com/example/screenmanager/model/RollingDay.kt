package com.example.screenmanager.model

import com.example.screenmanager.domain.TimeBuckets
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Jedan od poslednjih 7 dana, sa stvarnim datumom (ne fiksni mock datum).
 *
 * dayIndex ide od 0 (pre 6 dana) do 6 (danas) — isti indeks koji koristi
 * DailyUsage.day iz baze, tako da se lako mapiraju jedan na drugi.
 */
data class RollingDay(
    val dayIndex: Int,
    val startTimestamp: Long,
    val shortLabel: String,
    val dateLabel: String,
    val displayLabel: String
)

/**
 * Generiše poslednjih 7 dana (uklj. danas), redom od najstarijeg ka danas.
 */
fun generateRollingDays(now: Long = System.currentTimeMillis()): List<RollingDay> {
    val todayStart = TimeBuckets.startOfToday(now)
    val shortFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val dateFormat = SimpleDateFormat("d", Locale.getDefault())

    return (0..6).map { offset ->
        val dayIndex = offset
        val daysAgo = 6 - offset
        val timestamp = todayStart - daysAgo * 24 * 60 * 60 * 1000L
        RollingDay(
            dayIndex = dayIndex,
            startTimestamp = timestamp,
            shortLabel = shortFormat.format(timestamp),
            dateLabel = dateFormat.format(timestamp),
            displayLabel = if (daysAgo == 0) "Today" else shortFormat.format(timestamp)
        )
    }
}