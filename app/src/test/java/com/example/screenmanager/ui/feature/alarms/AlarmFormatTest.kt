package com.example.screenmanager.ui.feature.alarms

import com.example.screenmanager.model.AlarmRule
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek

class AlarmFormatTest {

    @Test
    fun `repeat tokens round trip through DayOfWeek`() {
        // Isti zapis koji očekuje AlarmScheduler ("Mon".."Sun").
        val tokens = setOf("Mon", "Wed", "Sun")
        val days = tokens.toDaysOfWeek()
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY), days)
        assertEquals(tokens, days.toRepeatTokens())
    }

    @Test
    fun `every day of week maps to the token the scheduler understands`() {
        assertEquals(
            setOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
            DayOfWeek.entries.toSet().toRepeatTokens()
        )
    }

    @Test
    fun `repeat summary`() {
        fun alarm(days: Set<String>) = AlarmRule(1, 7, 30, "", days, enabled = true)
        assertEquals("Once", alarm(emptySet()).repeatSummary())
        assertEquals("Weekdays", alarm(setOf("Mon", "Tue", "Wed", "Thu", "Fri")).repeatSummary())
        assertEquals("Weekends", alarm(setOf("Sat", "Sun")).repeatSummary())
        assertEquals("Tue, Thu", alarm(setOf("Thu", "Tue")).repeatSummary())
    }

    @Test
    fun `time until alarm picks the two largest units`() {
        assertEquals("in less than a minute", formatTimeUntil(20_000))
        assertEquals("in 45m", formatTimeUntil(45 * 60_000L))
        assertEquals("in 7h 20m", formatTimeUntil((7 * 60 + 20) * 60_000L))
        assertEquals("in 2d 3h", formatTimeUntil((2 * 24 * 60 + 3 * 60 + 10) * 60_000L))
    }
}
