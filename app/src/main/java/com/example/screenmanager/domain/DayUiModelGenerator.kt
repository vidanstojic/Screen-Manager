package com.example.screenmanager.domain

import com.example.screenmanager.model.DayUiModel
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Generiše poslednjih 7 dana (uklj. danas), redom od najstarijeg ka danas.
 *
 * Indeks u listi (0..6) se poklapa sa DailyUsage.day iz baze, jer oba
 * koriste TimeBuckets.startOfRollingWeek() kao referentnu tačku.
 */
fun generateLastSevenDays(now: Long = System.currentTimeMillis()): List<DayUiModel> {
    val today = TimeBuckets.epochDay(now)
    val shortFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val dateFormat = SimpleDateFormat("d", Locale.getDefault())

    return (6 downTo 0).map { daysAgo ->
        // Preko kalendara, ne "- n * 24h": dani oko DST prelaza nemaju 24h.
        val timestamp = TimeBuckets.startOfDay(today - daysAgo)
        DayUiModel(
            timestamp = timestamp,
            shortLabel = shortFormat.format(timestamp),
            dateLabel = dateFormat.format(timestamp),
            displayLabel = if (daysAgo == 0) "Today" else shortFormat.format(timestamp)
        )
    }
}