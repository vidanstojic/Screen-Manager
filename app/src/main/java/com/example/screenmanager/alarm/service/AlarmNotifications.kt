package com.example.screenmanager.alarm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.screenmanager.R
import com.example.screenmanager.domain.AlarmScheduler
import com.example.screenmanager.model.MissionType
import com.example.screenmanager.service.AlarmReceiver
import com.example.screenmanager.ui.common.formatClock

/**
 * Notifikacije Smart Alarm dela. Kanal za zvonjenje nema sopstveni zvuk —
 * zvuk pušta [AlarmPlayer] (da bi jačina, postepeno pojačavanje i utišavanje
 * tokom misije bili pod kontrolom aplikacije).
 */
class AlarmNotifications(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    init {
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_RINGING, "Ringing alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Shown while an alarm rings"
                    setSound(null, null)
                    enableVibration(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                },
                NotificationChannel(CHANNEL_STATUS, "Alarm status", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Snoozed and missed alarms"
                }
            )
        )
    }

    /** Notifikacija servisa dok alarm zvoni: preko celog ekrana na zaključanom telefonu. */
    fun ringing(ring: ActiveRing?): Notification {
        val alarm = ring?.alarm
        val title = alarm?.label?.ifBlank { null } ?: "Alarm"
        val builder = NotificationCompat.Builder(context, CHANNEL_RINGING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(alarm?.let { formatClock(it.hour, it.minute) } ?: "Ringing")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(ringScreenIntent())
            .setFullScreenIntent(ringScreenIntent(), true)
        if (ring != null && ring.canSnooze) {
            builder.addAction(0, "Snooze ${ring.alarm.snooze.minutes} min", serviceIntent(AlarmRingService.ACTION_SNOOZE))
        }
        // Sa misijom se alarm gasi samo na ekranu alarma, rešavanjem misije.
        if (ring != null && ring.alarm.mission.type == MissionType.NONE) {
            builder.addAction(0, "Dismiss", serviceIntent(AlarmRingService.ACTION_DISMISS))
        } else if (ring != null) {
            builder.addAction(0, "Solve to dismiss", ringScreenIntent())
        }
        return builder.build()
    }

    fun showSnoozed(alarmId: Long, label: String, until: Long, canDismiss: Boolean) {
        val builder = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("${label.ifBlank { "Alarm" }} snoozed")
            .setContentText("Rings again at ${formatClock(until)}")
            .setOngoing(true)
            .setSilent(true)
        if (canDismiss) {
            val dismiss = PendingIntent.getBroadcast(
                context,
                AlarmScheduler.requestCode(alarmId),
                Intent(context, AlarmReceiver::class.java).apply {
                    action = AlarmReceiver.ACTION_DISMISS_SNOOZED
                    putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "Dismiss now", dismiss)
        }
        manager.notify(snoozedId(alarmId), builder.build())
    }

    fun cancelSnoozed(alarmId: Long) = manager.cancel(snoozedId(alarmId))

    fun showMissed(label: String, ringingSince: Long) {
        val notification = NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Missed alarm")
            .setContentText("${label.ifBlank { "Alarm" }} rang at ${formatClock(ringingSince)}")
            .setAutoCancel(true)
            .build()
        manager.notify(MISSED_ID, notification)
    }

    private fun ringScreenIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, AlarmRingActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
        ),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun serviceIntent(action: String): PendingIntent = PendingIntent.getService(
        context,
        action.hashCode(),
        Intent(context, AlarmRingService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun snoozedId(alarmId: Long) = SNOOZED_ID_BASE + (AlarmScheduler.requestCode(alarmId) and 0xFFFF)

    companion object {
        const val CHANNEL_RINGING = "alarm_ringing"
        const val CHANNEL_STATUS = "alarm_status"
        const val RINGING_ID = 41_001
        private const val MISSED_ID = 41_002
        private const val SNOOZED_ID_BASE = 42_000_000
    }
}
