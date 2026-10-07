package com.example.screenmanager.domain.rules

import com.example.screenmanager.model.ShortVideoConfig
import com.example.screenmanager.model.ShortsMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ShortsPolicyTest {
    private val zone = ZoneId.of("Europe/Belgrade")
    private val ig = "com.instagram.android"
    private val t0 = LocalDateTime.parse("2026-09-29T12:00:00").atZone(zone).toInstant().toEpochMilli()
    private val midnight = LocalDateTime.parse("2026-09-30T00:00:00").atZone(zone).toInstant().toEpochMilli()

    private fun visible(config: ShortVideoConfig, state: SessionState?, now: Long) =
        ShortsPolicy.onVisible(config, ig, state, now, zone, midnight)

    /** Simulira gledanje Reels-a sa skeniranjem na 2 s u intervalu [from, to). */
    private fun watch(config: ShortVideoConfig, start: SessionState?, from: Long, to: Long): Pair<SessionState?, ShortsVerdict> {
        var state = start
        var verdict: ShortsVerdict = ShortsVerdict.Allow
        var t = from
        while (t < to) {
            val (next, v) = visible(config, state, t)
            state = next
            verdict = v
            t += 2_000
        }
        return state to verdict
    }

    @Test
    fun `blocked mode always kicks without touching state`() {
        val (state, verdict) = visible(ShortVideoConfig(mode = ShortsMode.BLOCKED), null, t0)
        assertEquals(null, state)
        assertTrue(verdict is ShortsVerdict.Kick)
        assertEquals(null, (verdict as ShortsVerdict.Kick).until)
    }

    @Test
    fun `budget mode is left to the service`() {
        assertEquals(ShortsVerdict.Allow, visible(ShortVideoConfig(mode = ShortsMode.BUDGET), null, t0).second)
    }

    @Test
    fun `sessions mode allows M minutes then kicks for K minutes`() {
        val config = ShortVideoConfig(mode = ShortsMode.SESSIONS, sessionLengthMinutes = 1, maxSessions = 2, cooldownMinutes = 10)
        val (during, allow) = watch(config, null, t0, t0 + 58_000)
        assertEquals(ShortsVerdict.Allow, allow)
        assertEquals(ShortsPolicy.stateKey(ig), during!!.ruleId)

        val (afterM, kick) = watch(config, during, t0 + 58_000, t0 + 64_000)
        assertTrue(kick is ShortsVerdict.Kick)
        assertEquals(afterM!!.frozenUntil, (kick as ShortsVerdict.Kick).until)

        // Tokom pauze i dalje izbacuje.
        val (_, stillKick) = visible(config, afterM, t0 + 5 * 60_000)
        assertTrue(stillKick is ShortsVerdict.Kick)
        // Posle pauze nova sesija.
        val (second, allowAgain) = visible(config, afterM, afterM.frozenUntil + 1)
        assertEquals(ShortsVerdict.Allow, allowAgain)
        assertEquals(2, second!!.sessionsUsed)
    }

    @Test
    fun `sessions mode locks shorts until midnight after the last session`() {
        val config = ShortVideoConfig(mode = ShortsMode.SESSIONS, sessionLengthMinutes = 1, maxSessions = 1, cooldownMinutes = 0)
        val (state, _) = watch(config, null, t0, t0 + 64_000)
        val (_, verdict) = visible(config, state, t0 + 2 * 60 * 60_000)
        assertEquals(ShortsVerdict.Kick("All Shorts/Reels sessions for today are used up", midnight), verdict)
    }
}
