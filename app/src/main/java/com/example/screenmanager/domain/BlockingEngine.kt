package com.example.screenmanager.domain

import com.example.screenmanager.model.AppLimitRule
import com.example.screenmanager.model.EmergencySessionConfig
import com.example.screenmanager.model.ScheduleRule
import com.example.screenmanager.model.ShortVideoConfig
import com.example.screenmanager.model.WakeUpConfig
import com.example.screenmanager.data.local.AppUsageLog
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class BlockingDecision(
    val shouldBlock: Boolean,
    val reason: BlockingReason? = null,
    val blockDurationMinutes: Int = 0,
    val unblockTime: LocalDateTime? = null
)

enum class BlockingReason {
    APP_LIMIT_EXCEEDED,
    SHORTS_LIMIT_EXCEEDED,
    SCHEDULED_BLOCK,
    WAKE_UP_BLOCK,
    NONE
}

class BlockingEngine(
    private val usageLogs: List<AppUsageLog>
) {
    fun evaluateBlock(
        packageName: String,
        appLimitRules: List<AppLimitRule>,
        shortVideoConfig: ShortVideoConfig?,
        scheduleRules: List<ScheduleRule>,
        wakeUpConfig: WakeUpConfig?,
        emergencySession: EmergencySessionConfig?,
        isShortFormContent: Boolean = false
    ): BlockingDecision {
        // Check emergency session first - it overrides all blocks
        if (emergencySession?.isActive == true) {
            return BlockingDecision(shouldBlock = false, reason = BlockingReason.NONE)
        }

        // Check if package is in an active app limit rule
        val appLimitRule = appLimitRules.firstOrNull { rule ->
            rule.isEnabled && packageName in rule.selectedAppIds
        }
        if (appLimitRule != null) {
            val todayUsage = getTodayUsageMinutes(packageName)
            if (todayUsage >= appLimitRule.dailyLimitMinutes) {
                val lastBlock = getLastBlockTimestamp(packageName, BlockingReason.APP_LIMIT_EXCEEDED)
                val blockEnd = lastBlock?.let { it.plusMinutes(appLimitRule.blockDurationMinutes.toLong()) }
                    ?: LocalDateTime.now().plusMinutes(appLimitRule.blockDurationMinutes.toLong())
                
                if (LocalDateTime.now().isBefore(blockEnd)) {
                    return BlockingDecision(
                        shouldBlock = true,
                        reason = BlockingReason.APP_LIMIT_EXCEEDED,
                        blockDurationMinutes = appLimitRule.blockDurationMinutes,
                        unblockTime = blockEnd
                    )
                }
            }
        }

        // Check if short-form content and shorts are blocked
        if (isShortFormContent && shortVideoConfig?.isEnabled == true) {
            if (packageName in shortVideoConfig.selectedAppIds) {
                val shortsUsageToday = getShortFormUsageMinutes(packageName)
                if (shortsUsageToday >= shortVideoConfig.maxReelsWatchMinutes) {
                    val blockEnd = LocalDateTime.now().plusMinutes(shortVideoConfig.fullAppBlockMinutes.toLong())
                    return BlockingDecision(
                        shouldBlock = true,
                        reason = BlockingReason.SHORTS_LIMIT_EXCEEDED,
                        blockDurationMinutes = shortVideoConfig.fullAppBlockMinutes,
                        unblockTime = blockEnd
                    )
                }
            }
        }

        // Check scheduled blocks
        val now = LocalDateTime.now()
        val activeScheduleRule = scheduleRules.firstOrNull { rule ->
            rule.isEnabled && packageName in rule.selectedAppIds && isTimeInRange(
                now.toLocalTime(),
                rule.startTime,
                rule.endTime
            ) && now.dayOfWeek in rule.daysOfWeek
        }
        if (activeScheduleRule != null) {
            return BlockingDecision(
                shouldBlock = true,
                reason = BlockingReason.SCHEDULED_BLOCK,
                unblockTime = now.toLocalDate().atTime(activeScheduleRule.endTime)
            )
        }

        // Check wake-up block
        if (wakeUpConfig?.isEnabled == true && packageName in wakeUpConfig.selectedAppIds) {
            val lastActivity = getLastPhoneActivity()
            val inactivityMinutes = getInactivityMinutes(lastActivity)
            
            if (inactivityMinutes >= wakeUpConfig.inactivityHours * 60) {
                val blockEnd = LocalDateTime.now().plusMinutes(wakeUpConfig.blockDurationMinutes.toLong())
                return BlockingDecision(
                    shouldBlock = true,
                    reason = BlockingReason.WAKE_UP_BLOCK,
                    blockDurationMinutes = wakeUpConfig.blockDurationMinutes,
                    unblockTime = blockEnd
                )
            }
        }

        return BlockingDecision(shouldBlock = false, reason = BlockingReason.NONE)
    }

    private fun getTodayUsageMinutes(packageName: String): Int {
        val today = LocalDateTime.now().toLocalDate()
        return usageLogs
            .filter { log ->
                log.packageName == packageName &&
                log.timestamp?.toLocalDate() == today
            }
            .sumOf { it.durationMs / 1000 / 60 }.toInt()
    }

    private fun getShortFormUsageMinutes(packageName: String): Int {
        val today = LocalDateTime.now().toLocalDate()
        return usageLogs
            .filter { log ->
                log.packageName == packageName &&
                log.timestamp?.toLocalDate() == today &&
                log.isShortForm
            }
            .sumOf { it.durationMs / 1000 / 60 }.toInt()
    }

    private fun getLastBlockTimestamp(packageName: String, reason: BlockingReason): LocalDateTime? {
        // This would query from a block history table if available
        // For now, return null
        return null
    }

    private fun getLastPhoneActivity(): LocalDateTime {
        // This would be provided by device sensors (screen off/on)
        // For now, return current time
        return LocalDateTime.now()
    }

    private fun getInactivityMinutes(lastActivity: LocalDateTime): Int {
        val minutes = java.time.temporal.ChronoUnit.MINUTES.between(lastActivity, LocalDateTime.now())
        return minutes.toInt()
    }

    private fun isTimeInRange(current: LocalTime, start: LocalTime, end: LocalTime): Boolean {
        return if (start.isBefore(end)) {
            !current.isBefore(start) && current.isBefore(end)
        } else {
            // Crosses midnight
            !current.isBefore(start) || current.isBefore(end)
        }
    }
}
