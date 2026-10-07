package com.example.screenmanager.domain

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.screenmanager.data.apps.InstalledAppsSource
import com.example.screenmanager.data.local.ScreenManagerDatabase
import com.example.screenmanager.data.repository.AlarmRepository
import com.example.screenmanager.data.repository.AppLimitRulesRepository
import com.example.screenmanager.data.repository.EmergencySessionConfigRepository
import com.example.screenmanager.data.repository.RuntimeStateRepository
import com.example.screenmanager.data.repository.ScheduleRulesRepository
import com.example.screenmanager.data.repository.SessionLimitRulesRepository
import com.example.screenmanager.data.repository.SettingsRepository
import com.example.screenmanager.data.repository.ShortVideoConfigRepository
import com.example.screenmanager.data.repository.WakeUpConfigRepository
import com.example.screenmanager.data.repository.WakeUpLockoutController
import com.example.screenmanager.domain.rules.RulesEngine
import com.example.screenmanager.domain.wakeup.WakeUpLockoutTrigger
import com.example.screenmanager.worker.UsageAggregationWorker
import java.util.concurrent.TimeUnit

/**
 * Centralna fabrika zavisnosti za UI i pozadinske komponente.
 *
 * Repozitorijumi su stateless omotači nad DAO-ima (Room je singleton), pa
 * se [SettingsRepository] kešira samo da svi slojevi dele iste Flow-ove.
 */
object ServiceLocator {
    @Volatile
    private var settingsRepository: SettingsRepository? = null

    @Volatile
    private var installedApps: InstalledAppsSource? = null

    fun database(context: Context) = ScreenManagerDatabase.getInstance(context)

    fun usageStatsRepository(context: Context): UsageStatsRepository {
        val db = database(context)
        return UsageStatsRepository(
            context.applicationContext,
            db.appUsageLogDao(),
            db.usageHourlyDao(),
            db.appInternalStateDao()
        )
    }

    fun runtimeStateRepository(context: Context): RuntimeStateRepository {
        val db = database(context)
        return RuntimeStateRepository(db.appInternalStateDao(), db.sessionLimitDao())
    }

    fun settingsRepository(context: Context): SettingsRepository {
        return settingsRepository ?: synchronized(this) {
            settingsRepository ?: run {
                val db = database(context)
                SettingsRepository(
                    AppLimitRulesRepository(db.appLimitRuleDao()),
                    ShortVideoConfigRepository(db.shortVideoConfigDao()),
                    ScheduleRulesRepository(db.scheduleRuleDao()),
                    WakeUpConfigRepository(db.wakeUpConfigDao()),
                    EmergencySessionConfigRepository(db.emergencySessionConfigDao()),
                    SessionLimitRulesRepository(db.sessionLimitDao())
                ).also { settingsRepository = it }
            }
        }
    }

    fun wakeUpLockoutTrigger(context: Context): WakeUpLockoutTrigger =
        WakeUpLockoutController(settingsRepository(context), runtimeStateRepository(context))

    fun rulesEngine(): RulesEngine = RulesEngine()

    fun alarmRepository(context: Context): AlarmRepository {
        val db = database(context)
        return AlarmRepository(db.alarmDao())
    }

    fun alarmScheduler(context: Context): AlarmScheduler {
        return AlarmScheduler(context.applicationContext)
    }

    fun permissionStateChecker(context: Context) = PermissionStateChecker(context.applicationContext)

    /** Keširano: imena i lista instaliranih aplikacija se dele između ekrana. */
    fun installedApps(context: Context): InstalledAppsSource {
        return installedApps ?: synchronized(this) {
            installedApps ?: InstalledAppsSource(context).also { installedApps = it }
        }
    }

    /**
     * Periodični worker: sync + retencija. Ovo je rezervni mehanizam —
     * glavni sync radi FocusMonitorService svakih ~60s dok je ekran upaljen.
     */
    fun scheduleDailyMaintenance(context: Context) {
        val request = PeriodicWorkRequestBuilder<UsageAggregationWorker>(6, TimeUnit.HOURS)
            .addTag(UsageAggregationWorker.TAG)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            UsageAggregationWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
