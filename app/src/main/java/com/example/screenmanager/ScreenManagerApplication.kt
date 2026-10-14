package com.example.screenmanager

import android.app.Application
import android.util.Log
import com.example.screenmanager.alarm.data.DirectBootAlarmStore
import com.example.screenmanager.data.DefaultRulesData
import com.example.screenmanager.data.local.AppInternalState
import com.example.screenmanager.domain.AppStateKeys
import com.example.screenmanager.domain.ServiceLocator
import com.example.screenmanager.ui.common.AppIconLoader
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Globalni Application sloj.
 *
 * Asinhrono (bez blokiranja main thread-a) upisuje podrazumevane podatke
 * (vidi [initializeDefaultData]). Zavisnosti se dobijaju preko [ServiceLocator],
 * pa UI, servisi i workeri dele iste instance.
 */
class ScreenManagerApplication : Application() {

    // SupervisorJob: pad jednog seed-a ne obara ostale.
    private val applicationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            Log.e("ScreenManagerApp", "Greška pri inicijalizaciji podrazumevanih podataka", throwable)
        }
    )

    /** Proces ostaje živ zbog FocusMonitorService-a; kad se UI sakrije, oslobađaju se UI keševi. */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN) AppIconLoader.trim()
    }

    override fun onCreate() {
        super.onCreate()
        initializeDefaultData()
    }

    /**
     * Primeri pravila (rasporedi, dnevni limiti, interval) upisuju se SAMO pri prvom
     * pokretanju posle instalacije. Ranije su se ponovo upisivali čim bi korisnik
     * obrisao sva pravila jedne vrste, pa su se obrisana pravila vraćala.
     *
     * Jednoredne konfiguracije (Shorts, jutarnja blokada, emergency) su obavezne i
     * dopunjuju se uvek kad nedostaju.
     */
    private fun initializeDefaultData() {
        // Alarm može da pokrene proces pre prvog otključavanja (direct boot); baza tada nije dostupna.
        if (!DirectBootAlarmStore.isUserUnlocked(this)) return
        val database = ServiceLocator.database(this)

        applicationScope.launch {
            val state = database.appInternalStateDao()
            if (state.getLong(AppStateKeys.DEFAULT_RULES_SEEDED) == null) {
                // Postojeća instalacija (pre ove verzije) već ima Shorts konfiguraciju: ne seed-ujemo ponovo.
                val freshInstall = database.shortVideoConfigDao().get() == null
                if (freshInstall) {
                    DefaultRulesData.getDefaultScheduleRules().forEach { database.scheduleRuleDao().upsert(it) }
                    DefaultRulesData.getDefaultAppLimitRules().forEach { database.appLimitRuleDao().upsert(it) }
                    DefaultRulesData.getDefaultSessionLimitRules().forEach { database.sessionLimitDao().upsertRule(it) }
                }
                state.upsert(AppInternalState(AppStateKeys.DEFAULT_RULES_SEEDED, 1L))
            }

            val shortVideo = database.shortVideoConfigDao()
            if (shortVideo.get() == null) shortVideo.upsert(DefaultRulesData.getDefaultShortVideoConfig())

            val wakeUp = database.wakeUpConfigDao()
            if (wakeUp.get() == null) wakeUp.upsert(DefaultRulesData.getDefaultWakeUpConfig())

            val emergency = database.emergencySessionConfigDao()
            if (emergency.get() == null) emergency.upsert(DefaultRulesData.getDefaultEmergencySessionConfig())
        }
    }
}
