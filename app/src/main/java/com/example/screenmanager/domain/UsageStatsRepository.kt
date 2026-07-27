package com.example.screenmanager.domain

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.screenmanager.data.local.AppInternalState
import com.example.screenmanager.data.local.AppInternalStateDao
import com.example.screenmanager.data.local.AppUsageLog
import com.example.screenmanager.data.local.AppUsageLogDao
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class UsageStatsRepository(
    private val context: Context,
    private val usageLogDao: AppUsageLogDao,
    private val stateDao: AppInternalStateDao
) {
    private val usageStatsManager: UsageStatsManager
        get() = context.getSystemService(UsageStatsManager::class.java)

    fun observeTodayHourlyUsage() = usageLogDao.observeHourlyUsage(TimeBuckets.startOfToday())
    fun observeDailyTotals() = usageLogDao.observeTotalsSince(TimeBuckets.startOfToday())
    fun observeWeeklyTotals() = usageLogDao.observeTotalsSince(TimeBuckets.startOfWeek())
    fun observeMonthlyTotals() = usageLogDao.observeTotalsSince(TimeBuckets.startOfMonth())

    suspend fun syncUsageEvents(now: Long = System.currentTimeMillis()) {
        val fallbackStart = TimeBuckets.startOfToday(now)
        val lastSync = stateDao.getLong(AppStateKeys.LAST_USAGE_EVENT_SYNC_AT) ?: fallbackStart
        val from = lastSync.coerceAtLeast(now - TimeUnit.DAYS.toMillis(7))
        val logs = readForegroundSessions(from, now)
        usageLogDao.insertAll(logs)
        stateDao.upsert(AppInternalState(AppStateKeys.LAST_USAGE_EVENT_SYNC_AT, now))
    }

    suspend fun recordSession(packageName: String, start: Long, end: Long) {
        if (packageName == context.packageName) return
        val duration = end - start
        if (duration > 1_000) {
            usageLogDao.insert(
                AppUsageLog(
                    packageName = packageName,
                    startTimeStamp = start,
                    endTimeStamp = end,
                    durationMs = duration
                )
            )
        }
    }

    fun findLastForegroundPackage(from: Long, to: Long): String? {
        val events = usageStatsManager.queryEvents(from, to)
        val event = UsageEvents.Event()
        var lastPackage: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            ) {
                lastPackage = event.packageName
            }
        }
        return lastPackage
    }

    suspend fun cleanupOldLogs(now: Long = System.currentTimeMillis()) {
        usageLogDao.deleteOlderThan(now - TimeUnit.DAYS.toMillis(90))
    }

    private fun readForegroundSessions(from: Long, to: Long): List<AppUsageLog> {
        val events = usageStatsManager.queryEvents(from, to)
        val event = UsageEvents.Event()
        val activeStarts = mutableMapOf<String, Long>()
        val logs = mutableListOf<AppUsageLog>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val packageName = event.packageName ?: continue
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND,
                UsageEvents.Event.ACTIVITY_RESUMED -> activeStarts[packageName] = event.timeStamp

                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val start = activeStarts.remove(packageName) ?: continue
                    val duration = event.timeStamp - start
                    if (duration > 1_000 && packageName != context.packageName) {
                        logs += AppUsageLog(
                            packageName = packageName,
                            startTimeStamp = start,
                            endTimeStamp = event.timeStamp,
                            durationMs = duration
                        )
                    }
                }
            }
        }

        return logs
    }
}
