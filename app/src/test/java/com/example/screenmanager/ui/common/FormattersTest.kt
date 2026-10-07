package com.example.screenmanager.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class FormattersTest {

    @Test
    fun `duration shows hours and minutes only when present`() {
        assertEquals("0m", formatDuration(0))
        assertEquals("<1m", formatDuration(30_000))
        assertEquals("42m", formatDuration(42 * 60_000L))
        assertEquals("2h", formatDuration(2 * 60 * 60_000L))
        assertEquals("3h 42m", formatDuration((3 * 60 + 42) * 60_000L))
    }

    @Test
    fun `duration never goes negative`() {
        assertEquals("0m", formatDuration(-5_000))
    }

    @Test
    fun `countdown switches to hours format above one hour`() {
        assertEquals("05:09", formatCountdown(309_000))
        assertEquals("1:05:09", formatCountdown(3_909_000))
        assertEquals("00:00", formatCountdown(-1))
    }

    @Test
    fun `clock is always two digits`() {
        assertEquals("07:05", formatClock(7, 5))
        assertEquals("22:00", formatClock(LocalTime.of(22, 0)))
    }

    @Test
    fun `pluralize adds s only for counts other than one`() {
        assertEquals("1 app", pluralize(1, "app"))
        assertEquals("0 apps", pluralize(0, "app"))
        assertEquals("3 apps", pluralize(3, "app"))
    }
}
