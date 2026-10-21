package com.example.screenmanager.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.screenmanager.domain.PlatformCompat

/**
 * Vraća [FocusMonitorService] kada sistem ubije proces (BUG-018).
 *
 * `START_STICKY` nije dovoljan: Xiaomi/MIUI (i slični) bez "Autostart" dozvole
 * ne restartuju servis, pa su limiti i Shorts blokada stajali dok korisnik ne
 * otvori aplikaciju. Ovde se drži jedan alarm:
 * - posle svakog starta servisa → provera za [CHECK_INTERVAL_MS];
 * - kad se aplikacija skloni iz Recents → restart za [RESTART_DELAY_MS];
 * - kad sistem odbije start FGS-a → novi pokušaj za [RETRY_DELAY_MS].
 *
 * Alarm je `RTC` (ne budi telefon): dok je ekran ugašen servis ionako ne radi
 * ništa, a zakasneli alarm stiže čim se telefon probudi — baš kad treba.
 * Tačan alarm (uz "Alarms & reminders") daje i izuzetak za pokretanje FGS-a iz
 * pozadine na Androidu 12+.
 */
object MonitorKeepAlive {
    const val CHECK_INTERVAL_MS = 15 * 60_000L
    const val RESTART_DELAY_MS = 1_000L
    const val RETRY_DELAY_MS = 60_000L

    private const val REQUEST_CODE = 0x4D4B // "MK"; drugi receiver, pa nema sudara sa alarmima

    fun schedule(context: Context, delayMs: Long = CHECK_INTERVAL_MS) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val at = System.currentTimeMillis() + delayMs
        val operation = pendingIntent(context)
        try {
            if (PlatformCompat.canScheduleExactAlarms(alarmManager)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, at, operation)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, at, operation)
            }
        } catch (error: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, at, operation)
        }
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, MonitorKeepAliveReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}

/** Okidač [MonitorKeepAlive]: servis radi → samo sledeća provera; ne radi → start (on zakazuje dalje). */
class MonitorKeepAliveReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (FocusMonitorService.isRunning) {
            MonitorKeepAlive.schedule(context)
        } else {
            FocusMonitorService.start(context)
        }
    }
}
