package com.example.screenmanager.model

import androidx.compose.ui.graphics.Color

enum class UsageRange(val label: String) {
    Week("Week"),
    Day("Day")
}

enum class MainDestination(
    val navLabel: String,
    val icon: String
) {
    UsageStats("Usage\nStats", "▥"),
    UsageLimits("Usage\nLimits", "◴"),
    GeneralUsage("General\nUsage", "▦"),
    GeneralSettings("General\nSettings", "□"),
    AddLimit("Add\nLimit", "+");

    companion object {
        val bottomItems = listOf(UsageStats, UsageLimits, GeneralUsage, GeneralSettings, AddLimit)
    }
}

data class MockDayUsage(
    val shortLabel: String,
    val dateLabel: String,
    val displayLabel: String,
    val totalMinutes: Int,
    val hourlyMinutes: List<Int>
)

data class MockAppUsage(
    val name: String,
    val minutes: Int,
    val iconText: String,
    val iconColor: Color,
    val category: String
)

data class AppDetailStats(
    val usageMinutes: Int,
    val sessions: Int,
    val averageMinutes: Int,
    val previousAverageMinutes: Int,
    val trendLabel: String,
    val trendColor: Color,
    val limitStatus: String
)