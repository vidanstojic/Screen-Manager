package com.example.screenmanager.alarm.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Okidači wake-up check-a (vidi [WakeCheckController]). */
class WakeCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId <= 0L) return
        val round = intent.getIntExtra(EXTRA_ROUND, 1)
        val controller = WakeCheckController(context)
        when (intent.action) {
            ACTION_ASK -> controller.ask(alarmId, round)
            ACTION_CONFIRM -> controller.confirm(alarmId)
            ACTION_DEADLINE -> controller.deadline(alarmId, round)
        }
    }

    companion object {
        const val ACTION_ASK = "com.example.screenmanager.alarm.WAKE_CHECK_ASK"
        const val ACTION_CONFIRM = "com.example.screenmanager.alarm.WAKE_CHECK_CONFIRM"
        const val ACTION_DEADLINE = "com.example.screenmanager.alarm.WAKE_CHECK_DEADLINE"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ROUND = "round"
    }
}
