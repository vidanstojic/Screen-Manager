package com.example.screenmanager.alarm.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.screenmanager.alarm.AlarmModule
import com.example.screenmanager.domain.AlarmScheduler
import com.example.screenmanager.domain.PlatformCompat

/**
 * Samo AlarmManager okidači wake-up check-a — bez zvonjenja i bez UI-ja.
 *
 * Odvojeno od [WakeCheckController] da prijemnik za BOOT_COMPLETED
 * ([AlarmSystemEventsReceiver]) ni posredno ne referencira [AlarmRingService]:
 * Android 15 zabranjuje pokretanje `mediaPlayback` FGS-a iz BOOT_COMPLETED, a
 * Play-ova statička analiza prijavljuje svaku takvu putanju. Zvonjenje uvek ide
 * preko tačnog alarma ([WakeCheckReceiver] → [WakeCheckController.deadline]).
 */
class WakeCheckAlarms(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /** Posle restarta telefona: ponovo zakazuje provere sačuvane u runtime skladištu. */
    fun restoreAll(now: Long) {
        AlarmModule.runtimeStore(context).wakeChecks.value.values.forEach { check ->
            val deadline = check.deadline
            if (deadline == null) {
                setExact(maxOf(check.askAt, now + RESTORE_DELAY_MS), pendingIntent(WakeCheckReceiver.ACTION_ASK, check.alarmId, check.round))
            } else {
                setExact(maxOf(deadline, now + RESTORE_DELAY_MS), pendingIntent(WakeCheckReceiver.ACTION_DEADLINE, check.alarmId, check.round))
            }
        }
    }

    fun setExact(at: Long, operation: PendingIntent) {
        try {
            if (PlatformCompat.canScheduleExactAlarms(alarmManager)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
            }
        } catch (error: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        }
    }

    /** Otkazuje oba okidača za [alarmId] (PendingIntent se poredi bez extras-a). */
    fun cancel(alarmId: Long) {
        alarmManager.cancel(pendingIntent(WakeCheckReceiver.ACTION_ASK, alarmId, 0))
        alarmManager.cancel(pendingIntent(WakeCheckReceiver.ACTION_DEADLINE, alarmId, 0))
    }

    fun pendingIntent(action: String, alarmId: Long, round: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            AlarmScheduler.requestCode(alarmId),
            Intent(context, WakeCheckReceiver::class.java).apply {
                this.action = action
                putExtra(WakeCheckReceiver.EXTRA_ALARM_ID, alarmId)
                putExtra(WakeCheckReceiver.EXTRA_ROUND, round)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private companion object {
        const val RESTORE_DELAY_MS = 5_000L
    }
}
