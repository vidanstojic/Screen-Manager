package com.example.screenmanager.data

import com.example.screenmanager.data.local.AppLimitRuleEntity
import com.example.screenmanager.data.local.EmergencySessionConfigEntity
import com.example.screenmanager.data.local.ScheduleRuleEntity
import com.example.screenmanager.data.local.SessionLimitRuleEntity
import com.example.screenmanager.data.local.ShortVideoConfigEntity
import com.example.screenmanager.data.local.WakeUpConfigEntity
import java.time.DayOfWeek

object DefaultRulesData {
    fun getDefaultScheduleRules(): List<ScheduleRuleEntity> {
        return listOf(
            ScheduleRuleEntity(
                id = "schedule_1",
                name = "Night focus",
                startTimeHour = 22,
                startTimeMinute = 0,
                endTimeHour = 7,
                endTimeMinute = 0,
                daysOfWeek = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY",
                selectedAppIds = "com.google.android.youtube,com.instagram.android",
                isEnabled = true
            ),
            ScheduleRuleEntity(
                id = "schedule_2",
                name = "Work hours",
                startTimeHour = 9,
                startTimeMinute = 0,
                endTimeHour = 17,
                endTimeMinute = 0,
                daysOfWeek = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY",
                selectedAppIds = "com.google.android.youtube",
                isEnabled = false
            )
        )
    }

    fun getDefaultAppLimitRules(): List<AppLimitRuleEntity> {
        return listOf(
            AppLimitRuleEntity(
                id = "limit_1",
                name = "YouTube daily cap",
                selectedAppIds = "com.google.android.youtube",
                dailyLimitMinutes = 45,
                blockDurationMinutes = 45,
                isEnabled = true,
                description = "Blocks YouTube until midnight once the daily cap is used."
            ),
            AppLimitRuleEntity(
                id = "limit_2",
                name = "Instagram daily cap",
                selectedAppIds = "com.instagram.android",
                dailyLimitMinutes = 30,
                blockDurationMinutes = 30,
                isEnabled = true,
                description = "Blocks Instagram until midnight once the daily cap is used."
            )
        )
    }

    fun getDefaultShortVideoConfig(): ShortVideoConfigEntity {
        return ShortVideoConfigEntity(
            id = "shorts_global",
            maxReelsWatchMinutes = 15,
            fullAppBlockMinutes = 60,
            selectedAppIds = "com.instagram.android,com.google.android.youtube",
            isEnabled = true
        )
    }

    fun getDefaultWakeUpConfig(): WakeUpConfigEntity {
        return WakeUpConfigEntity(
            id = "wakeup_global",
            inactivityHours = 7,
            triggerDelayMinutes = 2,
            blockDurationMinutes = 30,
            selectedAppIds = "com.google.android.youtube,com.instagram.android",
            isEnabled = true
        )
    }

    fun getDefaultEmergencySessionConfig(): EmergencySessionConfigEntity {
        return EmergencySessionConfigEntity(
            id = "emergency_global",
            defaultDurationMinutes = 15,
            activeUntilMillis = null,
            manualEndEnabled = true
        )
    }

    /** Primer interval pravila (isključen): 5 sesija po 5 min, 15 min pauze. */
    fun getDefaultSessionLimitRules(): List<SessionLimitRuleEntity> {
        return listOf(
            SessionLimitRuleEntity(
                id = "session_1",
                name = "Instagram intervals",
                selectedAppIds = "com.instagram.android",
                sessionLengthMinutes = 5,
                maxSessions = 5,
                cooldownMinutes = 15,
                isEnabled = false
            )
        )
    }
}
