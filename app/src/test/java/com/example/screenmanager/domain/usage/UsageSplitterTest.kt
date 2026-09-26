package com.example.screenmanager.domain.usage

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class UsageSplitterTest {
    private val zone = ZoneId.of("Europe/Belgrade")

    private fun at(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `session is split on hour and day boundaries`() {
        val slices = UsageSplitter.split(at("2026-09-25T23:30:00"), at("2026-09-26T01:15:00"), zone)
        val day25 = LocalDateTime.parse("2026-09-25T00:00").toLocalDate().toEpochDay()
        assertEquals(
            listOf(
                HourSlice(day25, 23, 30 * 60_000L),
                HourSlice(day25 + 1, 0, 60 * 60_000L),
                HourSlice(day25 + 1, 1, 15 * 60_000L)
            ),
            slices
        )
    }

    @Test
    fun `fall back DST day merges the repeated hour`() {
        // 25.10.2026: 03:00 CEST -> 02:00 CET, sat 02h se ponavlja.
        val start = at("2026-10-25T01:30:00")
        val end = start + 3 * 60 * 60_000L // 3 realna sata
        val aggregated = UsageSplitter.aggregate(listOf(UsageSession("a", start, end)), zone)
        val day = LocalDateTime.parse("2026-10-25T00:00").toLocalDate().toEpochDay()
        assertEquals(30 * 60_000L, aggregated[HourlyKey("a", day, 1)]?.durationMs)
        assertEquals(120 * 60_000L, aggregated[HourlyKey("a", day, 2)]?.durationMs)
        assertEquals(30 * 60_000L, aggregated[HourlyKey("a", day, 3)]?.durationMs)
        assertEquals(3 * 60 * 60_000L, aggregated.values.sumOf { it.durationMs })
    }

    @Test
    fun `session start is counted once and older days are filtered`() {
        val start = at("2026-09-25T23:50:00")
        val end = at("2026-09-26T00:10:00")
        val today = LocalDateTime.parse("2026-09-26T00:00").toLocalDate().toEpochDay()
        val aggregated = UsageSplitter.aggregate(listOf(UsageSession("a", start, end)), zone, fromEpochDay = today)
        assertEquals(1, aggregated.size)
        assertEquals(HourlyAggregate(10 * 60_000L, 0), aggregated[HourlyKey("a", today, 0)])
    }
}
