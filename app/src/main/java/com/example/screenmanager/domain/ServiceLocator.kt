package com.example.screenmanager.domain

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.screenmanager.data.repository.AlarmRepository
import com.example.screenmanager.data.local.ScreenManagerDatabase
import com.example.screenmanager.data.repository.BlockRepository
import com.example.screenmanager.worker.UsageAggregationWorker
import java.util.concurrent.TimeUnit

/**
 * Centralna fabrika zavisnosti za UI i pozadinske komponente.
 *
 * Ekrani i ViewModel-i dobijaju repozitorijume preko ovog sloja, dok boot
 * tok koristi istu tačku za zakazivanje održavanja.
 */
object ServiceLocator {
    fun database(context: Context) = ScreenManagerDatabase.getInstance(context)

    /**
     * Pravi repository koji dashboard koristi za usage statistike.
     */
    fun usageStatsRepository(context: Context): UsageStatsRepository {
        val db = database(context)
        return UsageStatsRepository(
            context.applicationContext,
            db.appUsageLogDao(),
            db.appInternalStateDao()
        )
    }

    /**
     * Pravi repository za blok pravila i interne state vrednosti.
     */
    fun blockRepository(context: Context): BlockRepository {
        val db = database(context)
        return BlockRepository(db.blockConfigDao(), db.appInternalStateDao())
    }

    fun alarmRepository(context: Context): AlarmRepository {
        val db = database(context)
        return AlarmRepository(db.alarmDao())
    }

    fun alarmScheduler(context: Context): AlarmScheduler {
        return AlarmScheduler(context.applicationContext)
    }

    fun permissionStateChecker(context: Context) = PermissionStateChecker(context.applicationContext)

    /**
     * Zakazuje periodični maintenance posao za usage agregaciju.
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
