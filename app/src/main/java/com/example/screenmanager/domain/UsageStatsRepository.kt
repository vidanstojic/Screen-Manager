package com.example.screenmanager.domain

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.screenmanager.data.local.AppInternalState
import com.example.screenmanager.data.local.AppInternalStateDao
import com.example.screenmanager.data.local.AppUsageLog
import com.example.screenmanager.data.local.AppUsageLogDao
import com.example.screenmanager.data.local.DailyUsage
import com.example.screenmanager.data.local.HourlyUsage
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class UsageStatsRepository(
    private val context: Context,
    private val usageLogDao: AppUsageLogDao,
    private val stateDao: AppInternalStateDao
) {
    private val usageStatsManager: UsageStatsManager
        get() = context.getSystemService(UsageStatsManager::class.java)

    // --- Ukupno (sve aplikacije zbirno) ---

    fun observeTodayHourlyUsage(): Flow<List<HourlyUsage>> {
        val start = TimeBuckets.startOfToday()
        val end = start + TimeUnit.DAYS.toMillis(1)
        return usageLogDao.observeHourlyUsage(start, end)
    }

    /**
     * Po satu za bilo koji izabrani dan (ne samo danas) — koristi se kad
     * korisnik izabere prošli dan kroz DayPicker.
     */
    fun observeHourlyUsageForDay(dayStartTimestamp: Long): Flow<List<HourlyUsage>> {
        val end = dayStartTimestamp + TimeUnit.DAYS.toMillis(1)
        return usageLogDao.observeHourlyUsage(dayStartTimestamp, end)
    }

    fun observeDailyTotals() = usageLogDao.observeTotalsSince(TimeBuckets.startOfToday())
    fun observeWeeklyTotals() = usageLogDao.observeTotalsSince(TimeBuckets.startOfWeek())
    fun observeMonthlyTotals() = usageLogDao.observeTotalsSince(TimeBuckets.startOfMonth())

    /**
     * Ukupno po danu za poslednjih 7 dana — glavni "Week" grafik.
     */
    fun observeWeeklyDailyBreakdown(): Flow<List<DailyUsage>> =
        usageLogDao.observeDailyTotalsAllApps(TimeBuckets.startOfRollingWeek())

    // --- Jedna aplikacija ---

    /**
     * Po satu za konkretnu aplikaciju, za izabrani dan — app detail ekran.
     */
    fun observeHourlyUsageForApp(packageName: String, dayStartTimestamp: Long): Flow<List<HourlyUsage>> {
        val end = dayStartTimestamp + TimeUnit.DAYS.toMillis(1)
        return usageLogDao.observeHourlyUsageForApp(dayStartTimestamp, end, packageName)
    }

    /**
     * Po danu za konkretnu aplikaciju, poslednjih 7 dana — trend za tu app.
     */
    fun observeDailyUsageForApp(packageName: String): Flow<List<DailyUsage>> =
        usageLogDao.observeDailyUsageForApp(TimeBuckets.startOfRollingWeek(), packageName)

    // --- Sync i retencija ---

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

    /**
     * Čuvamo samo poslednjih 7 dana za sada. Kada dodamo weekly/monthly
     * rollup tabele, ova granica ostaje ista — rollup će čuvati sažete
     * podatke nezavisno, a ovo briše samo sirove (raw) logove.
     */
    suspend fun cleanupOldLogs(now: Long = System.currentTimeMillis()) {
        usageLogDao.deleteOlderThan(now - TimeUnit.DAYS.toMillis(7))
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