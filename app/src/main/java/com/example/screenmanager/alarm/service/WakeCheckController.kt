package com.example.screenmanager.alarm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.screenmanager.R
import com.example.screenmanager.alarm.AlarmModule
import com.example.screenmanager.alarm.domain.PendingWakeCheck
import com.example.screenmanager.alarm.domain.RingReason
import com.example.screenmanager.alarm.domain.WakeCheckPolicy
import com.example.screenmanager.domain.AlarmScheduler

/**
 * Tok wake-up check-a na Androidu:
 * 1. [schedule] posle gašenja alarma → tačan okidač u `askAt`;
 * 2. [ask] (okidač) → notifikacija preko celog ekrana + ekran "Are you awake?"
 *    i drugi okidač na isteku roka (2 min);
 * 3. [confirm] ("I'm awake") → sve se otkazuje;
 * 4. [deadline] bez potvrde → alarm ponovo zvoni ([RingReason.WAKE_CHECK]).
 *
 * Stanje je u [com.example.screenmanager.alarm.data.AlarmRuntimeStore], pa
 * preživljava gašenje procesa i restart telefona.
 */
class WakeCheckController(private val context: Context) {
    private val alarms = WakeCheckAlarms(context)
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val store = AlarmModule.runtimeStore(context)

    fun schedule(check: PendingWakeCheck) {
        store.putWakeCheck(check)
        alarms.setExact(check.askAt, alarms.pendingIntent(WakeCheckReceiver.ACTION_ASK, check.alarmId, check.round))
    }

    fun ask(alarmId: Long, round: Int) {
        val pending = store.wakeChecks.value[alarmId]?.takeIf { it.round == round } ?: return
        val asked = WakeCheckPolicy.asked(pending, System.currentTimeMillis())
        store.putWakeCheck(asked)
        alarms.setExact(asked.deadline!!, alarms.pendingIntent(WakeCheckReceiver.ACTION_DEADLINE, alarmId, round))
        notificationManager.notify(NOTIFICATION_ID, buildNotification(alarmId, round, asked.deadline))
        if (Settings.canDrawOverlays(context)) {
            runCatching { context.startActivity(WakeCheckActivity.intent(context, alarmId, round)) }
                .onFailure { Log.w(TAG, "Cannot open wake-up check screen", it) }
        }
    }

    fun confirm(alarmId: Long) = cancel(alarmId)

    fun deadline(alarmId: Long, round: Int) {
        val pending = store.wakeChecks.value[alarmId]
        if (!WakeCheckPolicy.shouldRingAgain(pending, alarmId, round)) return
        cancel(alarmId)
        AlarmRingService.ring(context, alarmId, RingReason.WAKE_CHECK, wakeCheckRound = round)
    }

    /** Otkazuje sve za [alarmId] (potvrda, brisanje alarma). */
    fun cancel(alarmId: Long) {
        alarms.cancel(alarmId)
        store.removeWakeCheck(alarmId)
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun buildNotification(alarmId: Long, round: Int, deadline: Long): Notification {
        ensureChannel()
        val screen = PendingIntent.getActivity(
            context,
            AlarmScheduler.requestCode(alarmId),
            WakeCheckActivity.intent(context, alarmId, round),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Are you awake?")
            .setContentText("Confirm within 2 minutes or the alarm rings again.")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setWhen(deadline)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setTimeoutAfter(WakeCheckPolicy.RESPONSE_WINDOW_MS)
            .setContentIntent(screen)
            .setFullScreenIntent(screen, true)
            .addAction(0, "I'm awake", alarms.pendingIntent(WakeCheckReceiver.ACTION_CONFIRM, alarmId, round))
            .build()
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Wake-up check", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Asks if you're awake a few minutes after an alarm"
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
            )
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "WakeCheck"
        private const val CHANNEL_ID = "wake_check"
        private const val NOTIFICATION_ID = 41_010
    }
}
