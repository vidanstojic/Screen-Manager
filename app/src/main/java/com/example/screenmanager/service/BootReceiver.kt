package com.example.screenmanager.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.screenmanager.domain.ServiceLocator

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        ServiceLocator.scheduleDailyMaintenance(context)
        ContextCompat.startForegroundService(
            context,
            Intent(context, FocusMonitorService::class.java)
        )
    }
}
