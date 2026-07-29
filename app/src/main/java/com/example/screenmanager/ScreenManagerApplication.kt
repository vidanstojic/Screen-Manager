package com.example.screenmanager

import android.app.Application
import android.util.Log
import com.example.screenmanager.data.DefaultRulesData
import com.example.screenmanager.data.local.ScreenManagerDatabase
import com.example.screenmanager.data.repository.AppLimitRulesRepository
import com.example.screenmanager.data.repository.EmergencySessionConfigRepository
import com.example.screenmanager.data.repository.ScheduleRulesRepository
import com.example.screenmanager.data.repository.SettingsRepository
import com.example.screenmanager.data.repository.ShortVideoConfigRepository
import com.example.screenmanager.data.repository.WakeUpConfigRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ScreenManagerApplication : Application() {

    private val database by lazy { ScreenManagerDatabase.getInstance(this) }

    private val appLimitRulesRepository by lazy {
        AppLimitRulesRepository(database.appLimitRuleDao())
    }

    private val shortVideoConfigRepository by lazy {
        ShortVideoConfigRepository(database.shortVideoConfigDao())
    }

    private val scheduleRulesRepository by lazy {
        ScheduleRulesRepository(database.scheduleRuleDao())
    }

    private val wakeUpConfigRepository by lazy {
        WakeUpConfigRepository(database.wakeUpConfigDao())
    }

    private val emergencySessionConfigRepository by lazy {
        EmergencySessionConfigRepository(database.emergencySessionConfigDao())
    }

    val settingsRepository by lazy {
        SettingsRepository(
            appLimitRulesRepository,
            shortVideoConfigRepository,
            scheduleRulesRepository,
            wakeUpConfigRepository,
            emergencySessionConfigRepository
        )
    }

    // Scope vezan za životni ciklus cele aplikacije.
    // SupervisorJob osigurava da pad jednog launch-a ne obori ostale.
    private val applicationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            Log.e("ScreenManagerApp", "Greška pri inicijalizaciji podrazumevanih podataka", throwable)
        }
    )

    companion object {
        private lateinit var instance: ScreenManagerApplication

        fun getInstance(): ScreenManagerApplication = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        initializeDefaultData()
    }

    private fun initializeDefaultData() {
        val scheduleDao = database.scheduleRuleDao()
        val appLimitDao = database.appLimitRuleDao()
        val shortVideoDao = database.shortVideoConfigDao()
        val wakeUpDao = database.wakeUpConfigDao()
        val emergencySessionDao = database.emergencySessionConfigDao()

        // Svaka provera je nezavisna od ostalih, pa ih pokrećemo paralelno
        applicationScope.launch {
            val scheduleRules = scheduleDao.observeAll().first()
            if (scheduleRules.isEmpty()) {
                DefaultRulesData.getDefaultScheduleRules().forEach {
                    scheduleDao.upsert(it)
                }
            }
        }

        applicationScope.launch {
            val appLimitRules = appLimitDao.observeAll().first()
            if (appLimitRules.isEmpty()) {
                DefaultRulesData.getDefaultAppLimitRules().forEach {
                    appLimitDao.upsert(it)
                }
            }
        }

        applicationScope.launch {
            val shortVideoConfig = shortVideoDao.get()
            if (shortVideoConfig == null) {
                shortVideoDao.upsert(DefaultRulesData.getDefaultShortVideoConfig())
            }
        }

        applicationScope.launch {
            val wakeUpConfig = wakeUpDao.get()
            if (wakeUpConfig == null) {
                wakeUpDao.upsert(DefaultRulesData.getDefaultWakeUpConfig())
            }
        }

        applicationScope.launch {
            val emergencySessionConfig = emergencySessionDao.get()
            if (emergencySessionConfig == null) {
                emergencySessionDao.upsert(DefaultRulesData.getDefaultEmergencySessionConfig())
            }
        }
    }
}