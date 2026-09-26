package com.example.screenmanager.domain.usage

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Deo sesije koji pada u jedan lokalni sat jednog lokalnog dana. */
data class HourSlice(
    val epochDay: Long,
    val hour: Int,
    val durationMs: Long
)

data class HourlyKey(
    val packageName: String,
    val epochDay: Long,
    val hour: Int
)

data class HourlyAggregate(
    val durationMs: Long,
    val sessionStarts: Int
)

/**
 * Seče sesije na granicama lokalnih sati (B6).
 *
 * Ranije je cela sesija pripisivana satu/danu u kom je POČELA, pa je
 * 2h YouTube-a od 23:30 završavalo kao 120 min u 23h. Granice se računaju
 * preko [ZoneId], pa su i DST prelazi (npr. 25.10.) ispravni: pri
 * pomeranju sata unazad, oba "02h" sata se sabiraju u hour = 2.
 */
object UsageSplitter {

    fun split(start: Long, end: Long, zone: ZoneId): List<HourSlice> {
        if (end <= start) return emptyList()
        val slices = ArrayList<HourSlice>()
        var cursor = start
        while (cursor < end) {
            val local = Instant.ofEpochMilli(cursor).atZone(zone)
            val nextHour = local.truncatedTo(ChronoUnit.HOURS).plusHours(1).toInstant().toEpochMilli()
            val sliceEnd = minOf(end, nextHour)
            slices += HourSlice(local.toLocalDate().toEpochDay(), local.hour, sliceEnd - cursor)
            cursor = sliceEnd
        }
        return slices
    }

    /**
     * Agregira sesije u satne bucket-e, zadržavajući samo dane >= [fromEpochDay].
     * Početak sesije se broji kao "session start" u satu u kom je počela.
     */
    fun aggregate(
        sessions: Iterable<UsageSession>,
        zone: ZoneId,
        fromEpochDay: Long = Long.MIN_VALUE
    ): Map<HourlyKey, HourlyAggregate> {
        val result = HashMap<HourlyKey, HourlyAggregate>()
        for (session in sessions) {
            split(session.start, session.end, zone).forEachIndexed { index, slice ->
                if (slice.epochDay < fromEpochDay) return@forEachIndexed
                val key = HourlyKey(session.packageName, slice.epochDay, slice.hour)
                val previous = result[key]
                result[key] = HourlyAggregate(
                    durationMs = (previous?.durationMs ?: 0L) + slice.durationMs,
                    sessionStarts = (previous?.sessionStarts ?: 0) + if (index == 0) 1 else 0
                )
            }
        }
        return result
    }
}
