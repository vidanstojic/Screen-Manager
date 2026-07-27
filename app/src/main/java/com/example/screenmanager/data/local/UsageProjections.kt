package com.example.screenmanager.data.local

data class UsageSummary(
    val packageName: String,
    val totalDurationMs: Long
)

data class HourlyUsage(
    val hour: Int,
    val durationMs: Long
)
