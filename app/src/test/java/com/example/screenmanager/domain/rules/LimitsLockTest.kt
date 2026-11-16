package com.example.screenmanager.domain.rules

import com.example.screenmanager.domain.rules.LimitsLock.Companion.DAY_MS
import com.example.screenmanager.domain.rules.LimitsLock.Companion.HOUR_MS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LimitsLockTest {
    private val wall = 1_000_000_000L
    private val elapsed = 50_000L
    private val lock = LimitsLock(lockedAtWall = wall, lockedAtElapsed = elapsed, durationMs = HOUR_MS)

    @Test
    fun `active until the duration passes`() {
        assertTrue(lock.isActive(wall + HOUR_MS - 1, elapsed + HOUR_MS - 1))
        assertFalse(lock.isActive(wall + HOUR_MS, elapsed + HOUR_MS))
    }

    @Test
    fun `moving the clock forward does not unlock in the same boot`() {
        assertTrue(lock.isActive(wall + 10 * DAY_MS, elapsed + 60_000))
        assertEquals(wall + 10 * DAY_MS + HOUR_MS - 60_000, lock.effectiveEnd(wall + 10 * DAY_MS, elapsed + 60_000))
    }

    @Test
    fun `after a reboot only the wall clock counts`() {
        assertTrue(lock.isActive(wall + HOUR_MS - 1, nowElapsed = 1_000))
        assertFalse(lock.isActive(wall + HOUR_MS, nowElapsed = 1_000))
    }

    @Test
    fun `an active lock is only extended, never shortened`() {
        val day = LimitsLock.lockOrExtend(null, wall, elapsed, DAY_MS)
        val shorter = LimitsLock.lockOrExtend(day, wall + 1_000, elapsed + 1_000, HOUR_MS)
        assertSame(day, shorter)
        val longer = LimitsLock.lockOrExtend(day, wall + 1_000, elapsed + 1_000, 7 * DAY_MS)
        assertEquals(7 * DAY_MS, longer.durationMs)
        val afterExpiry = LimitsLock.lockOrExtend(lock, wall + 2 * HOUR_MS, elapsed + 2 * HOUR_MS, HOUR_MS)
        assertEquals(wall + 2 * HOUR_MS, afterExpiry.lockedAtWall)
    }

    private data class Rule(val limit: Int, val isEnabled: Boolean)

    @Test
    fun `while locked only new rules and turning a rule on are allowed`() {
        val existing = Rule(limit = 30, isEnabled = false)
        val on: (Rule) -> Rule = { it.copy(isEnabled = true) }
        assertTrue(LimitsLock.allowsSave(locked = true, existing = null, updated = existing, enabledCopy = on))
        assertTrue(LimitsLock.allowsSave(locked = true, existing = existing, updated = existing.copy(isEnabled = true), enabledCopy = on))
        assertFalse(LimitsLock.allowsSave(locked = true, existing = existing, updated = existing.copy(limit = 60), enabledCopy = on))
        val enabled = existing.copy(isEnabled = true)
        assertFalse(LimitsLock.allowsSave(locked = true, existing = enabled, updated = enabled.copy(isEnabled = false), enabledCopy = on))
        assertTrue(LimitsLock.allowsSave(locked = false, existing = enabled, updated = enabled.copy(isEnabled = false), enabledCopy = on))
    }
}
