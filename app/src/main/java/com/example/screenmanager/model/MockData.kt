package com.example.screenmanager.model

import androidx.compose.ui.graphics.Color

object MockUsage {
    val days = listOf(
        MockDayUsage("Wed", "15", "Wednesday", 301, listOf(18, 32, 41, 35, 24, 18, 12, 9, 8, 7, 12, 30, 48, 54, 42, 20, 14, 10, 8, 11, 24, 36, 31, 17)),
        MockDayUsage("Thu", "16", "Thursday", 287, listOf(12, 18, 20, 18, 16, 10, 8, 6, 7, 12, 20, 35, 40, 46, 38, 24, 18, 14, 10, 16, 22, 28, 30, 20)),
        MockDayUsage("Fri", "17", "Friday", 263, listOf(10, 14, 18, 16, 12, 9, 7, 8, 10, 18, 28, 34, 32, 26, 22, 18, 14, 20, 32, 38, 28, 20, 14, 8)),
        MockDayUsage("Sat", "18", "Saturday", 436, listOf(20, 24, 35, 42, 36, 18, 10, 8, 14, 26, 44, 60, 72, 78, 68, 44, 38, 52, 64, 70, 58, 42, 30, 22)),
        MockDayUsage("Sun", "19", "Sunday", 344, listOf(16, 20, 24, 30, 26, 18, 10, 8, 12, 20, 32, 48, 54, 50, 42, 34, 26, 22, 30, 38, 46, 40, 28, 18)),
        MockDayUsage("Mon", "20", "Monday", 226, listOf(8, 10, 12, 14, 10, 8, 6, 5, 8, 14, 18, 24, 30, 26, 22, 18, 12, 10, 14, 20, 28, 24, 16, 10)),
        MockDayUsage("Tue", "21", "Today", 290, listOf(28, 18, 32, 60, 70, 16, 2, 8, 8, 8, 8, 8, 8, 8, 8, 6, 4, 28, 46, 50, 48, 56, 40, 9))
    )

    val weekUsage = days
    const val dailyAverageMinutes = 265

    private val dayApps = listOf(
        MockAppUsage("Podešavanja", 99, "P", Color(0xFF7D97A7), "System"),
        MockAppUsage("PUBG MOBILE", 63, "PB", Color(0xFF6A5B45), "Game"),
        MockAppUsage("Instagram", 46, "IG", Color(0xFFD13ED8), "Social"),
        MockAppUsage("YouTube", 46, "YT", Color(0xFFE53935), "Video"),
        MockAppUsage("Chrome", 23, "CH", Color(0xFF3F8DF6), "Browser"),
        MockAppUsage("WhatsApp", 13, "WA", Color(0xFF1BAF5D), "Messaging")
    )

    private val weekApps = listOf(
        MockAppUsage("Instagram", 384, "IG", Color(0xFFD13ED8), "Social"),
        MockAppUsage("YouTube", 321, "YT", Color(0xFFE53935), "Video"),
        MockAppUsage("PUBG MOBILE", 282, "PB", Color(0xFF6A5B45), "Game"),
        MockAppUsage("Chrome", 176, "CH", Color(0xFF3F8DF6), "Browser"),
        MockAppUsage("Podešavanja", 126, "P", Color(0xFF7D97A7), "System"),
        MockAppUsage("WhatsApp", 93, "WA", Color(0xFF1BAF5D), "Messaging")
    )

    fun appsFor(range: UsageRange, selectedDay: MockDayUsage): List<MockAppUsage> {
        if (range == UsageRange.Week) return weekApps
        val offset = days.indexOf(selectedDay).coerceAtLeast(0)
        return dayApps.mapIndexed { index, app ->
            val adjusted = (app.minutes * (0.72f + ((offset + index) % 5) * 0.09f)).toInt().coerceAtLeast(4)
            app.copy(minutes = adjusted)
        }.sortedByDescending { it.minutes }
    }

    fun detailsFor(
        app: MockAppUsage,
        range: UsageRange,
        selectedDay: MockDayUsage
    ): AppDetailStats {
        val weekly = weeklyForApp(app)
        val usage = if (range == UsageRange.Week) weekly.sum() else hourlyForApp(app, selectedDay).sum()
        val sessions = if (range == UsageRange.Week) {
            weekly.sumOf { (it / 18).coerceAtLeast(1) }
        } else {
            (usage / 16).coerceAtLeast(1)
        }
        val average = (weekly.sum() / 7f).toInt()
        val previousAverage = (average * previousWeekFactor(app)).toInt().coerceAtLeast(1)
        val trendPercent = ((average - previousAverage) / previousAverage.toFloat() * 100).toInt()
        val trendUp = trendPercent >= 0
        return AppDetailStats(
            usageMinutes = usage,
            sessions = sessions,
            averageMinutes = average,
            previousAverageMinutes = previousAverage,
            trendLabel = if (trendUp) "+$trendPercent%" else "$trendPercent%",
            trendColor = if (trendUp) Color(0xFFFFB06A) else Color(0xFF8BE0B0),
            limitStatus = if (app.minutes > 60) "High" else "OK"
        )
    }

    fun hourlyForApp(app: MockAppUsage, selectedDay: MockDayUsage): List<Int> {
        val seed = (app.name.length + days.indexOf(selectedDay).coerceAtLeast(0)).coerceAtLeast(1)
        val desiredTotal = app.minutesFor(selectedDay)
        val raw = selectedDay.hourlyMinutes.mapIndexed { index, value ->
            val pulse = if ((index + seed) % 5 == 0) 1.35f else if ((index + seed) % 3 == 0) 0.72f else 0.92f
            (value * pulse).toInt().coerceAtLeast(0)
        }
        val rawTotal = raw.sum().coerceAtLeast(1)
        return raw.map { ((it / rawTotal.toFloat()) * desiredTotal).toInt().coerceAtLeast(0) }
    }

    fun weeklyForApp(app: MockAppUsage): List<Int> {
        return days.mapIndexed { index, day ->
            val base = app.minutesFor(day)
            val weekendBoost = if (day.shortLabel in setOf("Sat", "Sun")) 1.18f else 0.92f
            val appShift = 0.82f + ((app.name.length + index) % 4) * 0.12f
            (base * weekendBoost * appShift).toInt().coerceAtLeast(4)
        }
    }

    private fun MockAppUsage.minutesFor(day: MockDayUsage): Int {
        val dayIndex = days.indexOf(day).coerceAtLeast(0)
        val base = dayApps.firstOrNull { it.name == name }?.minutes ?: minutes
        val multiplier = 0.74f + ((dayIndex + name.length) % 6) * 0.08f
        return (base * multiplier).toInt().coerceAtLeast(3)
    }

    private fun previousWeekFactor(app: MockAppUsage): Float {
        return when (app.category) {
            "Game" -> 0.78f
            "Social" -> 1.14f
            "Video" -> 0.91f
            "Browser" -> 1.08f
            else -> 0.96f
        }
    }
}