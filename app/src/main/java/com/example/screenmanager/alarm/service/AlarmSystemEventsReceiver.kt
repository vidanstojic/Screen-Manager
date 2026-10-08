package com.example.screenmanager.alarm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.screenmanager.alarm.AlarmModule
import com.example.screenmanager.domain.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Vraća runtime stanje alarma posle događaja sistema:
 * - restart telefona / nova verzija aplikacije: zakazana odlaganja (redovne
 *   termine vraća postojeći [com.example.screenmanager.service.BootReceiver]);
 * - promena vremena ili vremenske zone: ponovo računa sve termine — alarm
 *   u 07:00 treba da zvoni u 07:00 po NOVOM lokalnom vremenu.
 *
 * Odlaganje čiji je trenutak prošao dok je telefon bio ugašen zvoni odmah.
 */
class AlarmSystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in HANDLED_ACTIONS) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val scheduler = ServiceLocator.alarmScheduler(context)
                if (action == Intent.ACTION_TIME_CHANGED || action == Intent.ACTION_TIMEZONE_CHANGED) {
                    scheduler.rescheduleAll(ServiceLocator.alarmRepository(context).getEnabled())
                }
                val now = System.currentTimeMillis()
                AlarmModule.runtimeStore(context).snoozes.value.values.forEach { snooze ->
                    scheduler.scheduleSnooze(snooze.alarmId, maxOf(snooze.at, now + MISSED_DELAY_MS), snooze.count)
                }
            } catch (error: RuntimeException) {
                Log.e(TAG, "Restoring alarm state after $action failed", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "AlarmSystemEvents"
        const val MISSED_DELAY_MS = 5_000L
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
    }
}
