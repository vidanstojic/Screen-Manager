package com.example.screenmanager.alarm.domain

import com.example.screenmanager.model.WakeCheckConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeCheckPolicyTest {

    private val on = WakeCheckConfig(enabled = true, delayMinutes = 5)

    @Test
    fun `first check is scheduled after the configured delay`() {
        val check = WakeCheckPolicy.afterDismiss(on, alarmId = 7, reason = RingReason.SCHEDULED, currentRound = 0, now = 1_000)!!
        assertEquals(1, check.round)
        assertEquals(1_000 + 5 * 60_000L, check.askAt)
        assertNull(check.deadline)
    }

    @Test
    fun `dismissing after a snooze also schedules the first check`() {
        assertEquals(1, WakeCheckPolicy.afterDismiss(on, 7, RingReason.SNOOZE, 0, 0)!!.round)
    }

    @Test
    fun `no check when disabled or testing`() {
        assertNull(WakeCheckPolicy.afterDismiss(WakeCheckConfig(enabled = false), 7, RingReason.SCHEDULED, 0, 0))
        assertNull(WakeCheckPolicy.afterDismiss(on, 7, RingReason.TEST, 0, 0))
    }

    @Test
    fun `re-ring after a missed check leads to the next round, up to the limit`() {
        assertEquals(2, WakeCheckPolicy.afterDismiss(on, 7, RingReason.WAKE_CHECK, currentRound = 1, now = 0)!!.round)
        assertEquals(3, WakeCheckPolicy.afterDismiss(on, 7, RingReason.WAKE_CHECK, currentRound = 2, now = 0)!!.round)
        assertNull(WakeCheckPolicy.afterDismiss(on, 7, RingReason.WAKE_CHECK, currentRound = WakeCheckPolicy.MAX_ROUNDS, now = 0))
    }

    @Test
    fun `asking starts the two minute window`() {
        val asked = WakeCheckPolicy.asked(PendingWakeCheck(7, 1, askAt = 0), now = 10_000)
        assertEquals(10_000 + 120_000L, asked.deadline)
    }

    @Test
    fun `alarm rings again only for the unconfirmed, asked check of the same round`() {
        val asked = PendingWakeCheck(7, round = 2, askAt = 0, deadline = 120_000)
        assertTrue(WakeCheckPolicy.shouldRingAgain(asked, alarmId = 7, round = 2))
        assertFalse("confirmed (removed)", WakeCheckPolicy.shouldRingAgain(null, 7, 2))
        assertFalse("stale deadline of an older round", WakeCheckPolicy.shouldRingAgain(asked, 7, 1))
        assertFalse("other alarm", WakeCheckPolicy.shouldRingAgain(asked, 8, 2))
        assertFalse("not asked yet", WakeCheckPolicy.shouldRingAgain(asked.copy(deadline = null), 7, 2))
    }
}
