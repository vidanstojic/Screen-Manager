package com.example.screenmanager.domain.rules

import com.example.screenmanager.model.SessionLimitRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class SessionLimitTrackerTest {
    private val zone = ZoneId.of("Europe/Belgrade")
    private val rule = SessionLimitRule("s", "IG", listOf("ig"), sessionLengthMinutes = 1, maxSessions = 2, cooldownMinutes = 5)
    private val t0 = LocalDateTime.parse("2026-09-26T12:00:00").atZone(zone).toInstant().toEpochMilli()

    /** Simulira foreground tick-ove od 2s u intervalu [from, to). */
    private fun run(start: SessionState?, from: Long, to: Long, foreground: Boolean): SessionState? {
        var state = start
        var t = from
        while (t < to) {
            state = SessionLimitTracker.advance(rule, state, foreground, t, zone)
            t += 2_000
        }
        return state
    }

    @Test
    fun `session expires after M minutes and freezes for K`() {
        val state = run(null, t0, t0 + 62_000, foreground = true)!!
        assertEquals(1, state.sessionsUsed)
        assertFalse(state.isSessionRunning)
        assertTrue(state.frozenUntil in (t0 + 60_000 + 5 * 60_000)..(t0 + 62_000 + 5 * 60_000))
    }

    @Test
    fun `no new session starts while frozen, next one starts after cooldown`() {
        var state = run(null, t0, t0 + 62_000, foreground = true)!!
        state = run(state, t0 + 62_000, t0 + 120_000, foreground = true)!!
        assertEquals(1, state.sessionsUsed)
        state = SessionLimitTracker.advance(rule, state, true, state.frozenUntil + 1, zone)
        assertEquals(2, state.sessionsUsed)
        assertTrue(state.isSessionRunning)
    }

    @Test
    fun `leaving the app for longer than the grace period ends the session`() {
        var state = run(null, t0, t0 + 20_000, foreground = true)!!
        state = SessionLimitTracker.advance(rule, state, false, t0 + 25_000, zone)
        assertTrue("short absence keeps the session", state.isSessionRunning)
        state = SessionLimitTracker.advance(rule, state, false, t0 + 60_000, zone)
        assertFalse(state.isSessionRunning)
        assertEquals(t0 + 18_000 + 5 * 60_000, state.frozenUntil)
    }

    @Test
    fun `returning after a long gap ends the old session and applies cooldown`() {
        val running = run(null, t0, t0 + 20_000, foreground = true)!!
        // Ekran ugašen 3 min (duže od grace, kraće od K=5), app i dalje "na vrhu".
        val frozen = SessionLimitTracker.advance(rule, running, true, t0 + 3 * 60_000, zone)
        assertEquals(1, frozen.sessionsUsed)
        assertFalse(frozen.isSessionRunning)
        assertEquals(t0 + 18_000 + 5 * 60_000, frozen.frozenUntil)
        // Posle 10 min pauza je istekla → odmah počinje nova sesija.
        val next = SessionLimitTracker.advance(rule, running, true, t0 + 10 * 60_000, zone)
        assertEquals(2, next.sessionsUsed)
        assertTrue(next.isSessionRunning)
    }

    @Test
    fun `pool is exhausted after N sessions and resets next day`() {
        var state: SessionState? = null
        var t = t0
        repeat(2) {
            state = run(state, t, t + 62_000, foreground = true)
            t = state!!.frozenUntil + 1
        }
        state = SessionLimitTracker.advance(rule, state, true, t, zone)
        assertEquals(2, state!!.sessionsUsed)
        assertFalse(state!!.isSessionRunning)
        val tomorrow = t0 + 24 * 60 * 60_000L
        state = SessionLimitTracker.advance(rule, state, true, tomorrow, zone)
        assertEquals(1, state!!.sessionsUsed)
        assertTrue(state!!.isSessionRunning)
    }
}
