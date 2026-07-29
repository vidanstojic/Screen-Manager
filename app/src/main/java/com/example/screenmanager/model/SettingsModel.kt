package com.example.screenmanager.model

import java.time.DayOfWeek
import java.time.LocalTime

data class WakeUpConfig(
    val inactivityHours: Int = 7,
    val triggerDelayMinutes: Int = 2,
    val blockDurationMinutes: Int = 30,
    val selectedAppIds: List<String> = emptyList(),
    val isEnabled: Boolean = true
)

data class ShortVideoConfig(
    val maxReelsWatchMinutes: Int = 15,
    val fullAppBlockMinutes: Int = 60,
    val selectedAppIds: List<String> = listOf("instagram", "youtube"),
    val isEnabled: Boolean = true
)

data class ScheduleRule(
    val id: String,
    val name: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val daysOfWeek: Set<DayOfWeek>,
    val selectedAppIds: List<String> = emptyList(),
    val isEnabled: Boolean = true
)

data class AppLimitRule(
    val id: String,
    val name: String,
    val selectedAppIds: List<String>,
    val dailyLimitMinutes: Int,
    val blockDurationMinutes: Int,
    val isEnabled: Boolean = true,
    val description: String = ""
)

data class EmergencySessionConfig(
    val defaultDurationMinutes: Int = 15,
    val activeUntilLabel: String? = null,
    val manualEndEnabled: Boolean = true,
    val isActive: Boolean = false
)

data class AppOption(
    val id: String,
    val name: String,
    val category: String,
    val icon: String
)