package com.example.screenmanager.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.screenmanager.domain.ServiceLocator

/**
 * Periodični maintenance posao za usage podatke.
 *
 * Ne prikazuje UI, ali drži dashboard i detalje svežim tako što sinhronizuje
 * događaje i čisti stare logove.
 */
class UsageAggregationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    /**
     * Sinhronizuje usage događaje i uklanja stare zapise.
     */
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
