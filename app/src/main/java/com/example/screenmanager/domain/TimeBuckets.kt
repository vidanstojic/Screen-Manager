package com.example.screenmanager.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Kalendarske granice u lokalnoj zoni.
 *
 * Sve se računa preko java.time + [ZoneId], a ne dodavanjem 24h u
 * milisekundama — dani oko DST prelaza imaju 23 ili 25 sati (B6).
 */
object TimeBuckets {
    fun zone(): ZoneId = ZoneId.systemDefault()

    fun epochDay(millis: Long, zone: ZoneId = zone()): Long =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toEpochDay()

    fun startOfDay(epochDay: Long, zone: ZoneId = zone()): Long =
        LocalDate.ofEpochDay(epochDay).atStartOfDay(zone).toInstant().toEpochMilli()

    fun startOfToday(now: Long = System.currentTimeMillis(), zone: ZoneId = zone()): Long =
        startOfDay(epochDay(now, zone), zone)

    fun startOfNextDay(now: Long = System.currentTimeMillis(), zone: ZoneId = zone()): Long =
        startOfDay(epochDay(now, zone) + 1, zone)

    fun startOfMonth(now: Long = System.currentTimeMillis(), zone: ZoneId = zone()): Long {
        val first = LocalDate.ofEpochDay(epochDay(now, zone)).withDayOfMonth(1)
        return first.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    const val MINUTE_MS: Long = 60_000L
    const val HOUR_MS: Long = 60 * MINUTE_MS
    const val DAY_MS: Long = 24 * HOUR_MS

    /**
     * Koliko dana unazad sync čita događaje. Android čuva 10 dnevnih fajlova sa
     * događajima (potvrđeno `dumpsys usagestats database-info` na S25 FE), pa
     * čitamo ceo taj prozor; queryEvents jednostavno vrati ono što postoji.
     */
    const val SYNC_LOOKBACK_DAYS: Long = 10

    /** Retencija sirovih sesija; rollup-ovi se čuvaju duže (vidi [ROLLUP_RETENTION_DAYS]). */
    const val RAW_LOG_RETENTION_DAYS: Long = 12

    /** Satni rollup-ovi — omogućavaju mesečni prikaz (TRS 2.1, B10). */
    const val ROLLUP_RETENTION_DAYS: Long = 400
}
