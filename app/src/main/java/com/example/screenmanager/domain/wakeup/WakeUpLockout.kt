package com.example.screenmanager.domain.wakeup

/** Ko je okinuo jutarnju blokadu. */
enum class WakeUpSource {
    /** Heuristika u FocusMonitorService: dugo ugašen ekran → korisnik aktivan. */
    SCREEN_HEURISTIC,

    /** Smart Alarm (Phase 2): korisnik je ugasio alarm. */
    ALARM_DISMISSED,

    /** Ručno aktiviranje iz UI-a. */
    MANUAL
}

/**
 * Ugovor za pokretanje jutarnje blokade (TRS 2.5).
 *
 * Smart Alarm modul (Phase 2) zavisi samo od ovog interfejsa, ne od
 * implementacije screen-time engine-a.
 *
 * @return kraj blokade (epoch ms) ili null ako je pravilo isključeno / bez aplikacija.
 */
interface WakeUpLockoutTrigger {
    suspend fun trigger(source: WakeUpSource, now: Long = System.currentTimeMillis()): Long?
}
