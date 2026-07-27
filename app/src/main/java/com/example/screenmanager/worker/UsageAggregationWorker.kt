package com.example.screenmanager.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.screenmanager.domain.ServiceLocator

class UsageAggregationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return runCatching {
            val usageRepository = ServiceLocator.usageStatsRepository(applicationContext)
            usageRepository.syncUsageEvents()
            usageRepository.cleanupOldLogs()
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "usage_aggregation_maintenance"
        const val TAG = "UsageAggregationWorker"
    }
}
