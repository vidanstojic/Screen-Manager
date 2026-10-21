package com.example.screenmanager.service

import android.content.Context

/**
 * Otkriva da je sistem ugasio [FocusMonitorService] dok je radio (BUG-018).
 *
 * Servis beleži "otkucaj" pri startu i pri svakom sync-u (~60 s), a uredno
 * gašenje ga briše. Kad UI zatekne servis ugašen, a postoji otkucaj iz ovog
 * paljenja telefona, proces je ubijen spolja (Xiaomi/MIUI i sl.) — limiti i
 * Shorts blokada su do tada stajali. Dozvole to ne pokazuju (sve ostaju date),
 * pa se korisniku prikazuje upozorenje sa podešavanjima rada u pozadini.
 */
class MonitorHealth(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun beat(now: Long) {
        prefs.edit().putLong(KEY_HEARTBEAT, now).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_HEARTBEAT).apply()
    }

    /** UI pre pokretanja servisa: beleži gašenje ako je neočekivano. */
    fun reportIfStopped(serviceRunning: Boolean, now: Long, bootAt: Long) {
        val heartbeat = prefs.getLong(KEY_HEARTBEAT, 0L)
        val reportedAt = prefs.getLong(KEY_STOPPED_AT, 0L)
        if (MonitorHealthPolicy.isUnexpectedStop(serviceRunning, heartbeat, reportedAt, bootAt)) {
            prefs.edit().putLong(KEY_STOPPED_AT, now).apply()
        }
    }

    /** Zabeleženo gašenje koje korisnik još nije sklonio. */
    fun hasUnacknowledgedStop(): Boolean =
        prefs.getLong(KEY_STOPPED_AT, 0L) > prefs.getLong(KEY_ACKNOWLEDGED_AT, 0L)

    fun acknowledge(now: Long) {
        prefs.edit().putLong(KEY_ACKNOWLEDGED_AT, now).apply()
    }

    private companion object {
        const val PREFS_NAME = "monitor_health"
        const val KEY_HEARTBEAT = "heartbeat_at"
        const val KEY_STOPPED_AT = "unexpected_stop_at"
        const val KEY_ACKNOWLEDGED_AT = "acknowledged_at"
    }
}

/** Čista odluka (testira se bez Androida). */
object MonitorHealthPolicy {
    /**
     * @param heartbeatAt poslednji otkucaj; 0 = servis nikad nije radio ili je uredno ugašen
     * @param lastReportedAt kada je prethodno gašenje već zabeleženo
     * @param bootAt početak ovog paljenja telefona — otkucaj od pre restarta nije gašenje
     *   (posle restarta servis podiže BootReceiver, a korisnik može da otvori aplikaciju pre njega)
     */
    fun isUnexpectedStop(serviceRunning: Boolean, heartbeatAt: Long, lastReportedAt: Long, bootAt: Long): Boolean =
        !serviceRunning && heartbeatAt > 0L && heartbeatAt > lastReportedAt && heartbeatAt >= bootAt
}
