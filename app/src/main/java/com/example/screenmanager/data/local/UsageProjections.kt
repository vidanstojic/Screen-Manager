package com.example.screenmanager.data.local

data class UsageSummary(
    val packageName: String,
    val totalDurationMs: Long
)

data class HourlyUsage(
    val hour: Int,
    val durationMs: Long
)

/**
 * Agregirana potrošnja po danu (0 = prvi dan opsega, 6 = poslednji).
 *
 * Ponovo se koristi i za "ukupno svih aplikacija po danu" (bez filtera
 * po packageName) i za "jedna aplikacija po danu" (sa filterom).
 */
data class DailyUsage(
    val day: Int,
    val durationMs: Long
)