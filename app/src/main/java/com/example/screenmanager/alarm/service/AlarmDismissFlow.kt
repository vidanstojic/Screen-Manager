package com.example.screenmanager.alarm.service

import android.content.Context
import android.util.Log
import com.example.screenmanager.alarm.domain.RingReason
import com.example.screenmanager.domain.ServiceLocator
import com.example.screenmanager.domain.wakeup.WakeUpSource
import com.example.screenmanager.model.AlarmRule

/**
 * Šta se dešava kad korisnik ugasi alarm (posle rešene misije, ili odmah
 * ako misije nema). Jedno mesto za sve posledice gašenja.
 */
object AlarmDismissFlow {
    private const val TAG = "AlarmDismissFlow"

    suspend fun onDismissed(context: Context, alarm: AlarmRule, reason: RingReason, wakeCheckRound: Int) {
        if (reason == RingReason.TEST) return
        // Veza sa Screen Manager-om (TRS 2.5): gašenje alarma pali jutarnju blokadu.
        runCatching { ServiceLocator.wakeUpLockoutTrigger(context).trigger(WakeUpSource.ALARM_DISMISSED) }
            .onFailure { Log.e(TAG, "Wake-up lockout after alarm failed", it) }
    }
}
