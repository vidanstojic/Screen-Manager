package com.example.screenmanager.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.screenmanager.MainActivity
import com.example.screenmanager.R
import com.example.screenmanager.domain.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE_ALARM) return

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId <= 0L) return

        val label = intent.getStringExtra(EXTRA_ALARM_LABEL).orEmpty().ifBlank { "Alarm" }
        showNotification(context, alarmId, label)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarmRepository = ServiceLocator.alarmRepository(context)
                val alarmScheduler = ServiceLocator.alarmScheduler(context)
                val alarm = alarmRepository.findById(alarmId)
                if (alarm == null) {
                    alarmScheduler.cancelAlarm(alarmId)
                    return@launch
                }

                if (!alarm.enabled) {
                    alarmScheduler.cancelAlarm(alarmId)
                    return@launch
                }

                if (alarm.repeatDays.isEmpty()) {
                    alarmRepository.upsert(alarm.copy(enabled = false))
                    alarmScheduler.cancelAlarm(alarmId)
                } else {
                    alarmScheduler.scheduleAlarm(alarm)
                }
            } catch (error: IllegalStateException) {
                Log.e(TAG, "Alarm processing failed due to invalid state", error)
            } catch (error: SecurityException) {
                Log.e(TAG, "Alarm scheduling blocked by system policy", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, alarmId: Long, label: String) {
        val channelId = CHANNEL_ID
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(
                channelId,
                "Smart Alarms",
                NotificationManager.IMPORTANCE_HIGH
            )
        )

        val openIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(label)
            .setContentText("Alarm is active")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()

        notificationManager.notify(alarmId.toInt(), notification)
    }

    companion object {
        const val ACTION_FIRE_ALARM = "com.example.screenmanager.action.FIRE_ALARM"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"
        private const val CHANNEL_ID = "smart_alarms_channel"
        private const val TAG = "AlarmReceiver"
    }
}
