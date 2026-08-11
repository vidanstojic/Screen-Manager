package com.example.screenmanager.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.screenmanager.domain.ServiceLocator

/**
 * Reaktivira periodično održavanje kada se uređaj podigne.
 *
 * Nakon boot-a ponovo zakazuje usage maintenance da bi UI imao ažurne podatke
 * i blok logika ostala dosledna.
 */
class BootReceiver : BroadcastReceiver() {
    /**
     * Na boot_completed ponovo zakazuje pozadinsko održavanje.
     */
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        ServiceLocator.scheduleDailyMaintenance(context)
    }
}
