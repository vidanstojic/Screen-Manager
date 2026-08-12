package com.example.screenmanager.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.screenmanager.domain.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarmRepository = ServiceLocator.alarmRepository(context)
                val alarmScheduler = ServiceLocator.alarmScheduler(context)
                alarmScheduler.rescheduleAll(alarmRepository.getEnabled())
            } catch (error: IllegalStateException) {
                Log.e(TAG, "Failed to restore alarms after boot", error)
            } catch (error: SecurityException) {
                Log.e(TAG, "System blocked alarm restore after boot", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
