package com.example.screenmanager.alarm.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class NextAlarmCalculatorTest {

    private val zone = ZoneId.of("Europe/Belgrade")

    private fun at(text: String): Long = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()
    private fun show(millis: Long): String =
        ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), zone).toLocalDateTime().toString()

    private fun next(hour: Int, minute: Int, days: Set<DayOfWeek>, now: String) =
        show(NextAlarmCalculator.nextTrigger(hour, minute, days, at(now), zone))

    @Test
    fun `one-shot alarm later today rings today`() {
        assertEquals("2026-10-08T21:00", next(21, 0, emptySet(), "2026-10-08T13:30"))
    }

    @Test
    fun `one-shot alarm already passed rings tomorrow`() {
        assertEquals("2026-10-09T07:00", next(7, 0, emptySet(), "2026-10-08T13:30"))
    }

    @Test
    fun `alarm at exactly now moves to the next occurrence`() {
        assertEquals("2026-10-09T13:30", next(13, 30, emptySet(), "2026-10-08T13:30"))
    }

    @Test
    fun `repeating alarm picks nearest selected day`() {
        // 08.10.2026 je četvrtak.
        val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
        assertEquals("2026-10-09T07:00", next(7, 0, weekdays, "2026-10-08T13:30"))
        assertEquals("2026-10-12T07:00", next(7, 0, weekdays, "2026-10-09T08:00"))
    }

    @Test
    fun `same weekday after today's time passed waits a full week`() {
        assertEquals("2026-10-15T07:00", next(7, 0, setOf(DayOfWeek.THURSDAY), "2026-10-08T13:30"))
    }

    @Test
    fun `uses the given now, not the device clock`() {
        // Stari AlarmScheduler je za dane u nedelji čitao Calendar.getInstance() umesto `now`.
        assertEquals("2026-01-05T06:00", next(6, 0, setOf(DayOfWeek.MONDAY), "2026-01-01T10:00"))
    }

    @Test
    fun `spring forward gap rings right after the gap`() {
        // 29.03.2026: 02:00 → 03:00 u Beogradu; 02:30 ne postoji.
        assertEquals("2026-03-29T03:30", next(2, 30, emptySet(), "2026-03-29T00:10"))
    }

    @Test
    fun `fall back overlap rings at the first occurrence`() {
        // 25.10.2026: 03:00 → 02:00; 02:30 postoji dvaput, alarm zvoni u prvom.
        val millis = NextAlarmCalculator.nextTrigger(2, 30, emptySet(), at("2026-10-25T00:10"), zone)
        val zoned = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), zone)
        assertEquals("+02:00", zoned.offset.id)
        assertEquals(30, zoned.minute)
    }
}
