package com.example.screenmanager

import android.app.Application
import android.util.Log
import com.example.screenmanager.data.DefaultRulesData
import com.example.screenmanager.data.repository.SettingsRepository
import com.example.screenmanager.domain.ServiceLocator
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Globalni Application sloj.
 *
 * Asinhrono (bez blokiranja main thread-a) seed-uje podrazumevana pravila
 * ako baza još nema zapise. Zavisnosti se dobijaju preko [ServiceLocator],
 * pa UI, servisi i workeri dele iste instance.
 */
class ScreenManagerApplication : Application() {

    val settingsRepository: SettingsRepository
        get() = ServiceLocator.settingsRepository(this)

    // SupervisorJob: pad jednog seed-a ne obara ostale.
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
        val database = ServiceLocator.database(this)

        applicationScope.launch {
            val dao = database.scheduleRuleDao()
            if (dao.observeAll().first().isEmpty()) {
                DefaultRulesData.getDefaultScheduleRules().forEach { dao.upsert(it) }
            }
        }

        applicationScope.launch {
            val dao = database.appLimitRuleDao()
            if (dao.observeAll().first().isEmpty()) {
                DefaultRulesData.getDefaultAppLimitRules().forEach { dao.upsert(it) }
            }
        }

        applicationScope.launch {
            val dao = database.sessionLimitDao()
            if (dao.countRules() == 0) {
                DefaultRulesData.getDefaultSessionLimitRules().forEach { dao.upsertRule(it) }
            }
        }

        applicationScope.launch {
            val dao = database.shortVideoConfigDao()
            if (dao.get() == null) dao.upsert(DefaultRulesData.getDefaultShortVideoConfig())
        }

        applicationScope.launch {
            val dao = database.wakeUpConfigDao()
            if (dao.get() == null) dao.upsert(DefaultRulesData.getDefaultWakeUpConfig())
        }

        applicationScope.launch {
            val dao = database.emergencySessionConfigDao()
            if (dao.get() == null) dao.upsert(DefaultRulesData.getDefaultEmergencySessionConfig())
        }
    }
}
