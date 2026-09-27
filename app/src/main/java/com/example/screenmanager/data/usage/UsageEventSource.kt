package com.example.screenmanager.data.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import com.example.screenmanager.domain.usage.RawEventKind
import com.example.screenmanager.domain.usage.RawUsageEvent

/**
 * Tanak Android adapter oko [UsageStatsManager.queryEvents].
 *
 * Sva logika nad događajima je u čistom Kotlin-u (domain/usage), ovde se
 * samo prevode tipovi događaja.
 */
class UsageEventSource(context: Context) {
    private val appContext = context.applicationContext
    private val usageStatsManager: UsageStatsManager =
        appContext.getSystemService(UsageStatsManager::class.java)

    /**
     * Paketi koji se ne broje kao "korišćenje": sama aplikacija, svi
     * launcher-i i System UI. Launcher bi inače bio među top aplikacijama.
     */
    val excludedPackages: Set<String> by lazy {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val launchers = appContext.packageManager
            .queryIntentActivities(home, PackageManager.MATCH_ALL)
            .mapNotNull { it.activityInfo?.packageName }
        (launchers + appContext.packageName + SYSTEM_UI).toSet()
    }

    /**
     * Da li aplikacija trenutno sme da čita UsageEvents.
     *
     * BEZ ove dozvole `queryEvents` NE baca izuzetak nego vraća prazan
     * rezultat — sync bi to protumačio kao "nije bilo korišćenja".
     */
    fun hasAccess(): Boolean {
        val appOps = appContext.getSystemService(AppOpsManager::class.java)
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            appContext.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun read(from: Long, to: Long): List<RawUsageEvent> {
        if (to <= from) return emptyList()
        val events = usageStatsManager.queryEvents(from, to) ?: return emptyList()
        val event = UsageEvents.Event()
        val result = ArrayList<RawUsageEvent>()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val kind = when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> RawEventKind.RESUMED
                UsageEvents.Event.ACTIVITY_PAUSED -> RawEventKind.PAUSED
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> RawEventKind.SCREEN_OFF
                UsageEvents.Event.DEVICE_SHUTDOWN -> RawEventKind.SHUTDOWN
                else -> null
            } ?: continue
            result += RawUsageEvent(event.timeStamp, event.packageName, kind)
        }
        return result
    }

    private companion object {
        const val SYSTEM_UI = "com.android.systemui"
    }
}
