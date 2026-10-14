package com.example.screenmanager.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.screenmanager.MainActivity
import com.example.screenmanager.alarm.data.DirectBootAlarmStore
import com.example.screenmanager.alarm.domain.NextAlarmCalculator
import com.example.screenmanager.model.AlarmRule
import com.example.screenmanager.service.AlarmReceiver
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * Zakazivanje alarma kod sistema ([AlarmManager.setAlarmClock]: tačno vreme,
 * ikonica alarma u statusnoj traci, budi telefon iz Doze-a).
 *
 * Svaki alarm ima do dva zakazana okidača sa istim request kodom a različitom
 * akcijom (PendingIntent ih razlikuje po akciji): redovan termin i odlaganje.
 */
class AlarmScheduler(
    private val context: Context
) {
    private val alarmManager: AlarmManager
        get() = context.getSystemService(AlarmManager::class.java)

    private val directBoot by lazy { DirectBootAlarmStore(context) }

    fun scheduleAlarm(alarm: AlarmRule) {
        if (!alarm.enabled) {
            cancelAlarm(alarm.id)
            return
        }
        setExact(nextTriggerAt(alarm), firePendingIntent(alarm.id, AlarmReceiver.ACTION_FIRE_ALARM, snoozeCount = 0))
        directBoot.save(alarm)
    }

    /** Odlaganje: [alarmId] ponovo zvoni u [at]; [snoozeCount] = koje je ovo odlaganje zaredom. */
    fun scheduleSnooze(alarmId: Long, at: Long, snoozeCount: Int) {
        setExact(at, firePendingIntent(alarmId, AlarmReceiver.ACTION_SNOOZE_ALARM, snoozeCount))
    }

    fun cancelSnooze(alarmId: Long) {
        alarmManager.cancel(firePendingIntent(alarmId, AlarmReceiver.ACTION_SNOOZE_ALARM, 0))
    }

    /** Otkazuje redovan termin I odlaganje (brisanje ili isključivanje alarma). */
    fun cancelAlarm(alarmId: Long) {
        alarmManager.cancel(firePendingIntent(alarmId, AlarmReceiver.ACTION_FIRE_ALARM, 0))
        cancelSnooze(alarmId)
        directBoot.remove(alarmId)
    }

    fun rescheduleAll(alarms: List<AlarmRule>) {
        alarms.forEach { alarm ->
            alarmManager.cancel(firePendingIntent(alarm.id, AlarmReceiver.ACTION_FIRE_ALARM, 0))
            if (alarm.enabled) scheduleAlarm(alarm)
        }
    }

    /**
     * Tačan okidač u [triggerAt]. Bez dozvole za tačne alarme (korisnik ju je
     * oduzeo) pada na prozor od 1 min — alarm kasni najviše minut, ali zvoni.
     */
    fun setExact(triggerAt: Long, operation: PendingIntent) {
        try {
            if (!alarmManager.canScheduleExactAlarms()) {
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, 60_000L, operation)
                return
            }
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showAppIntent()), operation)
        } catch (securityException: SecurityException) {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, 60_000L, operation)
        }
    }

    private fun showAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        SHOW_APP_REQUEST_CODE,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun firePendingIntent(alarmId: Long, action: String, snoozeCount: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_COUNT, snoozeCount)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(alarmId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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

    companion object {
        private const val SHOW_APP_REQUEST_CODE = 99_999

        fun requestCode(alarmId: Long): Int = (alarmId xor (alarmId ushr 32)).toInt()
    }
}
