package com.example.screenmanager.alarm.service

import android.content.Context
import android.util.Log
import com.example.screenmanager.alarm.AlarmModule
import com.example.screenmanager.alarm.domain.RingReason
import com.example.screenmanager.alarm.domain.WakeCheckPolicy
import com.example.screenmanager.alarm.domain.WakeUpDetoxPolicy
import com.example.screenmanager.model.AlarmRule

/**
 * Šta se dešava kad korisnik ugasi alarm (posle rešene misije, ili odmah
 * ako misije nema). Jedno mesto za sve posledice gašenja:
 * - wake-up detox: blokada izabranih aplikacija i sajtova;
 * - wake-up check: posle nekoliko minuta pitanje "Da li si budan?".
 *
 * Probno zvonjenje (TEST) nema posledica.
 */
object AlarmDismissFlow {
    private const val TAG = "AlarmDismissFlow"

    suspend fun onDismissed(context: Context, alarm: AlarmRule, reason: RingReason, wakeCheckRound: Int) {
        if (reason == RingReason.TEST) return
        val now = System.currentTimeMillis()
        // Detox pali prvo gašenje; ponovno zvonjenje zbog wake-up check-a ga ne produžava.
        if (reason != RingReason.WAKE_CHECK) startDetox(context, alarm, now)
        scheduleWakeCheck(context, alarm, reason, wakeCheckRound, now)
    }

    private fun scheduleWakeCheck(context: Context, alarm: AlarmRule, reason: RingReason, round: Int, now: Long) {
        runCatching {
            WakeCheckPolicy.afterDismiss(alarm.wakeCheck, alarm.id, reason, round, now)
                ?.let(WakeCheckController(context)::schedule)
        }.onFailure { Log.e(TAG, "Scheduling wake-up check failed", it) }
    }

    private fun startDetox(context: Context, alarm: AlarmRule, now: Long) {
        runCatching {
            val store = AlarmModule.detoxStore(context)
            WakeUpDetoxPolicy.lockFor(alarm, now, store.lock.value)?.let(store::start)
        }.onFailure { Log.e(TAG, "Starting wake-up detox failed", it) }
    }
}
