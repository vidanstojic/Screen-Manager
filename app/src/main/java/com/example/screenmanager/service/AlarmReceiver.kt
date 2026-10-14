package com.example.screenmanager.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.screenmanager.alarm.AlarmModule
import com.example.screenmanager.alarm.data.DirectBootAlarmStore
import com.example.screenmanager.alarm.domain.RingReason
import com.example.screenmanager.alarm.service.AlarmDismissFlow
import com.example.screenmanager.alarm.service.AlarmNotifications
import com.example.screenmanager.alarm.service.AlarmRingService
import com.example.screenmanager.domain.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Prima okidače iz [com.example.screenmanager.domain.AlarmScheduler]:
 * - [ACTION_FIRE_ALARM]: redovan termin → pokreće zvonjenje i zakazuje
 *   sledeći termin (ili gasi alarm koji zvoni jednom);
 * - [ACTION_SNOOZE_ALARM]: isteklo odlaganje → ponovo zvoni isti alarm;
 * - [ACTION_DISMISS_SNOOZED]: "Dismiss now" na notifikaciji odloženog alarma
 *   (samo za alarme bez misije) → otkazuje odlaganje i gasi alarm.
 *
 * Samo zvonjenje (zvuk, ekran, misija) radi [AlarmRingService].
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId <= 0L) return

        when (intent.action) {
            ACTION_FIRE_ALARM -> {
                AlarmRingService.ring(context, alarmId, RingReason.SCHEDULED)
                rescheduleAfterFire(context, alarmId)
            }

            ACTION_SNOOZE_ALARM -> {
                AlarmModule.runtimeStore(context).removeSnooze(alarmId)
                AlarmRingService.ring(
                    context,
                    alarmId,
                    RingReason.SNOOZE,
                    snoozeCount = intent.getIntExtra(EXTRA_SNOOZE_COUNT, 0)
                )
            }

            ACTION_DISMISS_SNOOZED -> dismissSnoozed(context, alarmId)
        }
    }

    private fun dismissSnoozed(context: Context, alarmId: Long) {
        ServiceLocator.alarmScheduler(context).cancelSnooze(alarmId)
        AlarmModule.runtimeStore(context).removeSnooze(alarmId)
        AlarmNotifications(context).cancelSnoozed(alarmId)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarm = runCatching { ServiceLocator.alarmRepository(context).findById(alarmId) }.getOrNull()
                    ?: DirectBootAlarmStore(context).find(alarmId)
                alarm?.let { AlarmDismissFlow.onDismissed(context, it, RingReason.SNOOZE, wakeCheckRound = 0) }
            } catch (error: RuntimeException) {
                Log.e(TAG, "Dismissing snoozed alarm failed", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun rescheduleAfterFire(context: Context, alarmId: Long) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!DirectBootAlarmStore.isUserUnlocked(context)) {
                    rescheduleBeforeUnlock(context, alarmId)
                    return@launch
                }
                val alarmRepository = ServiceLocator.alarmRepository(context)
                val alarmScheduler = ServiceLocator.alarmScheduler(context)
                val alarm = alarmRepository.findById(alarmId)
                when {
                    alarm == null || !alarm.enabled -> alarmScheduler.cancelAlarm(alarmId)
                    // Alarm koji zvoni jednom se gasi; odlaganje (ako ga bude) i dalje radi.
                    alarm.repeatDays.isEmpty() -> alarmRepository.upsert(alarm.copy(enabled = false))
                    else -> alarmScheduler.scheduleAlarm(alarm)
                }
            } catch (error: SecurityException) {
                Log.e(TAG, "Alarm scheduling blocked by system policy", error)
            } catch (error: RuntimeException) {
                // Npr. baza nedostupna — bez ovoga bi izuzetak u korutini srušio proces dok alarm zvoni.
                Log.e(TAG, "Alarm processing failed", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Telefon je restartovan i još nije otključan: baza nije dostupna, pa se sledeći
     * termin računa iz kopije alarma. Jednokratni alarm se posle otključavanja
     * isključuje i u bazi ([com.example.screenmanager.service.BootReceiver]).
     */
    private fun rescheduleBeforeUnlock(context: Context, alarmId: Long) {
        val store = DirectBootAlarmStore(context)
        val alarm = store.find(alarmId) ?: return
        if (alarm.repeatDays.isEmpty()) {
            store.markFiredOnce(alarmId)
        } else {
            ServiceLocator.alarmScheduler(context).scheduleAlarm(alarm)
        }
    }

    companion object {
        const val ACTION_FIRE_ALARM = "com.example.screenmanager.action.FIRE_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.example.screenmanager.action.SNOOZE_ALARM"
        const val ACTION_DISMISS_SNOOZED = "com.example.screenmanager.action.DISMISS_SNOOZED"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_SNOOZE_COUNT = "extra_snooze_count"
        private const val TAG = "AlarmReceiver"
    }
}
