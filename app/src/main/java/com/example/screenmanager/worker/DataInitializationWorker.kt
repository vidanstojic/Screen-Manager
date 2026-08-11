package com.example.screenmanager.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.screenmanager.data.DefaultRulesData
import com.example.screenmanager.data.local.ScreenManagerDatabase
import kotlinx.coroutines.flow.first

/**
 * Pozadinska inicijalizacija početnih pravila i konfiguracija.
 *
 * Ovo podržava startup tok: ako UI dođe bez podataka, Worker obezbeđuje da
 * baza ima default pravila za ekrane limita i podešavanja.
 */
class DataInitializationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    /**
     * Popunjava default rule-ove i vraća success/failure za WorkManager.
     */
    override suspend fun doWork(): Result {
        return try {
            val database = ScreenManagerDatabase.getInstance(applicationContext)

            // Initialize default rules if not already present
            val appLimitDao = database.appLimitRuleDao()
            val scheduleDao = database.scheduleRuleDao()
            val shortVideoDao = database.shortVideoConfigDao()
            val wakeUpDao = database.wakeUpConfigDao()
            val emergencySessionDao = database.emergencySessionConfigDao()

            // Check if schedule rules are empty
            val scheduleRules = scheduleDao.observeAll().first()
            if (scheduleRules.isEmpty()) {
                DefaultRulesData.getDefaultScheduleRules().forEach {
                    scheduleDao.upsert(it)
                }
            }

            // Check if app limit rules are empty
            val appLimitRules = appLimitDao.observeAll().first()
            if (appLimitRules.isEmpty()) {
                DefaultRulesData.getDefaultAppLimitRules().forEach {
                    appLimitDao.upsert(it)
                }
            }

            // Check if short video config exists
            val shortVideoConfig = shortVideoDao.get()
            if (shortVideoConfig == null) {
                shortVideoDao.upsert(DefaultRulesData.getDefaultShortVideoConfig())
            }

            // Check if wake up config exists
            val wakeUpConfig = wakeUpDao.get()
            if (wakeUpConfig == null) {
                wakeUpDao.upsert(DefaultRulesData.getDefaultWakeUpConfig())
            }

            // Check if emergency session config exists
            val emergencySessionConfig = emergencySessionDao.get()
            if (emergencySessionConfig == null) {
                emergencySessionDao.upsert(DefaultRulesData.getDefaultEmergencySessionConfig())
            }

            Result.success()
        } catch (e: IllegalStateException) {
            // Hvatamo fatalne greške poput Room migracije (od verzije 1 do 3)
            // Vraćamo FAILURE kako WorkManager ne bi upao u beskonačnu petlju zakucavajući CPU
            Log.e(TAG, "Fatalna greška sa bazom (migracija nedostaje): ${e.message}", e)
            Result.failure()
        } catch (e: Exception) {
            // Ostale neočekivane greške
            Log.e(TAG, "Greška pri inicijalizaciji podrazumevanih podataka: ${e.message}", e)
            Result.failure()
        }
    }

    companion object {
        const val TAG = "DataInitializationWorker"
        const val UNIQUE_WORK_NAME = "data_initialization"
    }
}