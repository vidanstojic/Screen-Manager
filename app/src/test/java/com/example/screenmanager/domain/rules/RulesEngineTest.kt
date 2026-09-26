package com.example.screenmanager.domain.rules

import com.example.screenmanager.model.AppLimitRule
import com.example.screenmanager.model.EmergencySessionConfig
import com.example.screenmanager.model.ScheduleRule
import com.example.screenmanager.model.SessionLimitRule
import com.example.screenmanager.model.ShortVideoConfig
import com.example.screenmanager.model.WakeUpConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class RulesEngineTest {
    private val zone = ZoneId.of("Europe/Belgrade")
    private val engine = RulesEngine()
    private val yt = "com.google.android.youtube"
    private val ig = "com.instagram.android"

    private fun at(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    // 2026-09-24 je četvrtak.
    private val night = ScheduleRule(
        id = "n", name = "Night", startTime = LocalTime.of(22, 0), endTime = LocalTime.of(7, 0),
        daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY),
        selectedAppIds = listOf(yt)
    )

    @Test
    fun `overnight schedule belongs to the day it started`() {
        val rules = RulesSnapshot(schedules = listOf(night))
        val fridayEarly = engine.evaluate(yt, at("2026-09-25T02:00:00"), zone, rules, RuntimeState())
        assertEquals(BlockReason.SCHEDULE, fridayEarly?.reason)
        assertEquals(at("2026-09-25T07:00:00"), fridayEarly?.blockedUntil)
        assertNull(engine.evaluate(yt, at("2026-09-26T02:00:00"), zone, rules, RuntimeState()))
        assertNull(engine.evaluate(yt, at("2026-09-25T22:30:00"), zone, rules, RuntimeState()))
        assertEquals(
            at("2026-09-25T07:00:00"),
            engine.evaluate(yt, at("2026-09-24T22:30:00"), zone, rules, RuntimeState())?.blockedUntil
        )
    }

    @Test
    fun `daily limit counts the whole app group and blocks until midnight`() {
        val rule = AppLimitRule("l", "Social", listOf(yt, ig), dailyLimitMinutes = 30, blockDurationMinutes = 0)
        val rules = RulesSnapshot(appLimits = listOf(rule))
        val now = at("2026-09-26T15:00:00")
        val under = RuntimeState(todayUsageMs = mapOf(yt to 10 * 60_000L, ig to 19 * 60_000L))
        val over = RuntimeState(todayUsageMs = mapOf(yt to 10 * 60_000L, ig to 20 * 60_000L))
        assertNull(engine.evaluate(yt, now, zone, rules, under))
        val decision = engine.evaluate(yt, now, zone, rules, over)
        assertEquals(BlockReason.DAILY_LIMIT, decision?.reason)
        assertEquals(at("2026-09-27T00:00:00"), decision?.blockedUntil)
    }

    @Test
    fun `emergency session overrides everything, also across midnight`() {
        val now = at("2026-09-24T23:55:00")
        val rules = RulesSnapshot(
            schedules = listOf(night),
            emergency = EmergencySessionConfig().activated(now, durationMinutes = 30)
        )
        assertNull(engine.evaluate(yt, at("2026-09-25T00:10:00"), zone, rules, RuntimeState()))
        assertEquals(
            BlockReason.SCHEDULE,
            engine.evaluate(yt, at("2026-09-25T00:26:00"), zone, rules, RuntimeState())?.reason
        )
    }

    @Test
    fun `most restrictive decision wins`() {
        val now = at("2026-09-24T23:00:00")
        val rules = RulesSnapshot(
            schedules = listOf(night),
            shortVideo = ShortVideoConfig(selectedAppIds = listOf(yt)),
            wakeUp = WakeUpConfig(selectedAppIds = listOf(yt))
        )
        val state = RuntimeState(
            shortsPenaltyUntil = mapOf(yt to now + 60 * 60_000L),
            wakeUpBlockedUntil = now + 10 * 60_000L
        )
        assertEquals(BlockReason.SCHEDULE, engine.evaluate(yt, now, zone, rules, state)?.reason)
        assertNull(engine.evaluate(ig, now, zone, rules, state))
    }

    @Test
    fun `session cooldown and pool exhaustion are enforced`() {
        val rule = SessionLimitRule("s", "IG", listOf(ig), sessionLengthMinutes = 5, maxSessions = 2, cooldownMinutes = 10)
        val rules = RulesSnapshot(sessionLimits = listOf(rule))
        val now = at("2026-09-26T12:00:00")
        val day = LocalDateTime.parse("2026-09-26T00:00").toLocalDate().toEpochDay()

        val cooling = RuntimeState(sessionStates = mapOf("s" to SessionState("s", day, sessionsUsed = 1, frozenUntil = now + 60_000)))
        assertEquals(BlockReason.SESSION_COOLDOWN, engine.evaluate(ig, now, zone, rules, cooling)?.reason)

        val exhausted = RuntimeState(sessionStates = mapOf("s" to SessionState("s", day, sessionsUsed = 2)))
        val decision = engine.evaluate(ig, now, zone, rules, exhausted)
        assertEquals(BlockReason.SESSION_POOL_EXHAUSTED, decision?.reason)
        assertEquals(at("2026-09-27T00:00:00"), decision?.blockedUntil)

        val yesterday = RuntimeState(sessionStates = mapOf("s" to SessionState("s", day - 1, sessionsUsed = 2)))
        assertNull(engine.evaluate(ig, now, zone, rules, yesterday))
    }
}
