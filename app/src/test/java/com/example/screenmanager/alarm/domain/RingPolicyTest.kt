package com.example.screenmanager.alarm.domain

import com.example.screenmanager.model.SnoozeConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RingPolicyTest {

    @Test
    fun `snooze allowed until the limit is used up`() {
        val config = SnoozeConfig(enabled = true, minutes = 5, maxCount = 2)
        assertTrue(RingPolicy.canSnooze(config, 0))
        assertTrue(RingPolicy.canSnooze(config, 1))
        assertFalse(RingPolicy.canSnooze(config, 2))
        assertEquals(1, RingPolicy.snoozesLeft(config, 1))
    }

    @Test
    fun `disabled snooze is never allowed`() {
        val config = SnoozeConfig(enabled = false, maxCount = 5)
        assertFalse(RingPolicy.canSnooze(config, 0))
        assertEquals(0, RingPolicy.snoozesLeft(config, 0))
    }

    @Test
    fun `snooze until adds the configured minutes`() {
        assertEquals(1_000L + 10 * 60_000L, RingPolicy.snoozeUntil(1_000L, SnoozeConfig(minutes = 10)))
    }

    @Test
    fun `fade in rises linearly from the start fraction to full`() {
        assertEquals(RingPolicy.FADE_START_FRACTION, RingPolicy.volumeFraction(0, 30), 1e-6f)
        assertEquals(0.55f, RingPolicy.volumeFraction(15_000, 30), 1e-6f)
        assertEquals(1f, RingPolicy.volumeFraction(30_000, 30), 1e-6f)
        assertEquals(1f, RingPolicy.volumeFraction(90_000, 30), 1e-6f)
    }

    @Test
    fun `no fade means full volume immediately`() {
        assertEquals(1f, RingPolicy.volumeFraction(0, 0), 1e-6f)
    }

    @Test
    fun `sound is ducked only shortly after a mission interaction`() {
        assertFalse(RingPolicy.isDucked(lastMissionInteractionAt = 0, now = 10_000))
        assertTrue(RingPolicy.isDucked(lastMissionInteractionAt = 10_000, now = 20_000))
        assertFalse(RingPolicy.isDucked(lastMissionInteractionAt = 10_000, now = 10_000 + RingPolicy.DUCK_HOLD_MS))
    }

    @Test
    fun `stream volume never drops to zero`() {
        assertEquals(1, RingPolicy.streamVolumeIndex(0, 7))
        assertEquals(6, RingPolicy.streamVolumeIndex(80, 7))
        assertEquals(7, RingPolicy.streamVolumeIndex(100, 7))
    }
}
