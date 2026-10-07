package com.example.screenmanager.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.screenmanager.MainActivity
import com.example.screenmanager.model.AlarmRule
import com.example.screenmanager.service.AlarmReceiver
import java.util.Calendar

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
     */
    fun nextTriggerAt(alarm: AlarmRule, now: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
        }

        val repeatDays = alarm.repeatDays.mapNotNull { dayTokenToCalendarValue(it) }.toSet()
        if (repeatDays.isEmpty()) {
            if (calendar.timeInMillis <= now) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis
        }

        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val nowMinutes = Calendar.getInstance().run {
            get(Calendar.HOUR_OF_DAY) * 60 + get(Calendar.MINUTE)
        }
        val alarmMinutes = alarm.hour * 60 + alarm.minute
        val currentDayIndex = calendarDayToIndex(today)
        var bestOffset = Int.MAX_VALUE

        repeatDays.forEach { day ->
            val targetIndex = calendarDayToIndex(day)
            var offset = (targetIndex - currentDayIndex + 7) % 7
            if (offset == 0 && alarmMinutes <= nowMinutes) {
                offset = 7
            }
            if (offset < bestOffset) {
                bestOffset = offset
            }
        }

        calendar.add(Calendar.DAY_OF_YEAR, bestOffset)
        return calendar.timeInMillis
    }

    private fun dayTokenToCalendarValue(day: String): Int? {
        return when (day) {
            "Mon" -> Calendar.MONDAY
            "Tue" -> Calendar.TUESDAY
            "Wed" -> Calendar.WEDNESDAY
            "Thu" -> Calendar.THURSDAY
            "Fri" -> Calendar.FRIDAY
            "Sat" -> Calendar.SATURDAY
            "Sun" -> Calendar.SUNDAY
            else -> null
        }
    }

    private fun calendarDayToIndex(day: Int): Int {
        return when (day) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }
}
