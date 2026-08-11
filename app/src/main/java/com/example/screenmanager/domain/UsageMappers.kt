package com.example.screenmanager.domain

import android.content.Context
import com.example.screenmanager.data.local.HourlyUsage
import com.example.screenmanager.data.local.UsageSummary
import com.example.screenmanager.model.AppUsageSummary
import java.util.concurrent.TimeUnit

/**
 * Prevodi sirove Room projekcije (UsageSummary, HourlyUsage) u UI-friendly
 * modele koje dashboard komponente direktno konzumiraju.
 */
fun UsageSummary.toAppUsageSummary(context: Context): AppUsageSummary {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(totalDurationMs).toInt()
    return AppUsageSummary(
        packageName = packageName,
        name = AppIconLoader.getAppLabel(context, packageName),
        minutes = minutes,
        category = "App"
    )
}

fun List<UsageSummary>.toAppUsageSummaries(context: Context): List<AppUsageSummary> =
    map { it.toAppUsageSummary(context) }

/**
 * Popunjava sve sate 0..23 (nedostajući sati = 0 minuta), jer DAO vraća
 * samo sate koji imaju bar jedan log.
 */
fun List<HourlyUsage>.toHourlyMinutesList(): List<Int> {
    val byHour = associateBy { it.hour }
    return (0..23).map { hour ->
        val ms = byHour[hour]?.durationMs ?: 0L
        TimeUnit.MILLISECONDS.toMinutes(ms).toInt()
    }
}