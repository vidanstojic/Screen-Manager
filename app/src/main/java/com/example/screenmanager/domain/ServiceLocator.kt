package com.example.screenmanager.domain

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.screenmanager.data.local.ScreenManagerDatabase
import com.example.screenmanager.worker.UsageAggregationWorker
import java.util.concurrent.TimeUnit

object ServiceLocator {
    fun database(context: Context) = ScreenManagerDatabase.getInstance(context)

    fun usageStatsRepository(context: Context): UsageStatsRepository {
        val db = database(context)
        return UsageStatsRepository(
            context.applicationContext,
            db.appUsageLogDao(),
            db.appInternalStateDao()
        )
    }

    fun blockRepository(context: Context): BlockRepository {
        val db = database(context)
        return BlockRepository(db.blockConfigDao(), db.appInternalStateDao())
    }

    fun permissionStateChecker(context: Context) = PermissionStateChecker(context.applicationContext)

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
