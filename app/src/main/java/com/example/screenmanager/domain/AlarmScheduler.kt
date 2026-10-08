package com.example.screenmanager.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.screenmanager.MainActivity
import com.example.screenmanager.model.AlarmRule
import com.example.screenmanager.service.AlarmReceiver
import com.example.screenmanager.alarm.domain.NextAlarmCalculator
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

class AlarmScheduler(
    private val context: Context
) {
    private val alarmManager: AlarmManager
        get() = context.getSystemService(AlarmManager::class.java)

    fun scheduleAlarm(alarm: AlarmRule) {
        if (!alarm.enabled) {
            cancelAlarm(alarm.id)
            return
        }
        val triggerAt = nextTriggerAt(alarm)
        val alarmIntent = alarmPendingIntent(alarm.id, alarm.label)
        val openAppIntent = PendingIntent.getActivity(
            context,
            requestCode(alarm.id) + 99_999,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    60_000L,
                    alarmIntent
                )
                return
            }
            val info = AlarmManager.AlarmClockInfo(triggerAt, openAppIntent)
            alarmManager.setAlarmClock(info, alarmIntent)
        } catch (securityException: SecurityException) {
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                60_000L,
                alarmIntent
            )
        }
    }

    fun cancelAlarm(alarmId: Long) {
        alarmManager.cancel(alarmPendingIntent(alarmId, null))
    }

    fun rescheduleAll(alarms: List<AlarmRule>) {
        alarms.forEach { alarm ->
            cancelAlarm(alarm.id)
            if (alarm.enabled) {
                scheduleAlarm(alarm)
            }
        }
    }

    private fun alarmPendingIntent(alarmId: Long, label: String?): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            if (!label.isNullOrBlank()) {
                putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, label)
            }
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(alarmId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(alarmId: Long): Int {
        return (alarmId xor (alarmId ushr 32)).toInt()
    }

    /**
     * Sledeći trenutak kada [alarm] treba da zazvoni. Javno jer isti račun
     * koristi i UI ("Next alarm"), da prikaz i stvarno zakazivanje ne odstupe.
     * Račun je u [NextAlarmCalculator] (čist, testiran, DST-safe).
     */
    fun nextTriggerAt(alarm: AlarmRule, now: Long = System.currentTimeMillis()): Long =
        NextAlarmCalculator.nextTrigger(
            hour = alarm.hour,
            minute = alarm.minute,
            repeatDays = alarm.repeatDays.mapNotNull(::dayTokenToDayOfWeek).toSet(),
            now = now,
            zone = ZoneId.systemDefault()
        )

    private fun dayTokenToDayOfWeek(token: String): DayOfWeek? =
        DayOfWeek.entries.firstOrNull { it.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) == token }
}
