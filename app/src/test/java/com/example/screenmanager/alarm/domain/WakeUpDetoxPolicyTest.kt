package com.example.screenmanager.alarm.domain

import com.example.screenmanager.domain.rules.BlockReason
import com.example.screenmanager.domain.rules.RulesEngine
import com.example.screenmanager.domain.rules.RulesSnapshot
import com.example.screenmanager.domain.rules.RuntimeState
import com.example.screenmanager.model.AlarmRule
import com.example.screenmanager.model.EmergencySessionConfig
import com.example.screenmanager.model.WakeUpDetox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class WakeUpDetoxPolicyTest {

    private fun alarm(detox: WakeUpDetox, label: String = "Work") =
        AlarmRule(id = 1, hour = 7, minute = 0, label = label, repeatDays = emptySet(), enabled = true, detox = detox)

    @Test
    fun `no lock when detox is off or has nothing to block`() {
        assertNull(WakeUpDetoxPolicy.lockFor(alarm(WakeUpDetox(enabled = false, apps = listOf("a"))), 0, null))
        assertNull(WakeUpDetoxPolicy.lockFor(alarm(WakeUpDetox(enabled = true)), 0, null))
    }

    @Test
    fun `lock lasts the configured minutes and normalizes sites`() {
        val lock = WakeUpDetoxPolicy.lockFor(
            alarm(WakeUpDetox(enabled = true, apps = listOf("com.instagram.android"), sites = listOf("https://www.YouTube.com/shorts"), minutes = 30)),
            now = 1_000,
            existing = null
        )!!
        assertEquals(1_000 + 30 * 60_000L, lock.until)
        assertEquals(setOf("com.instagram.android"), lock.packages)
        assertEquals(setOf("youtube.com"), lock.domains)
        assertEquals("Work", lock.label)
    }

    @Test
    fun `second alarm never shortens an active lock and merges lists`() {
        val existing = AlarmDetoxLock(until = 100 * 60_000L, packages = setOf("a"), domains = setOf("x.com"), label = "First")
        val merged = WakeUpDetoxPolicy.lockFor(
            alarm(WakeUpDetox(enabled = true, apps = listOf("b"), minutes = 10)),
            now = 60_000L,
            existing = existing
        )!!
        assertEquals(100 * 60_000L, merged.until)
        assertEquals(setOf("a", "b"), merged.packages)
        assertEquals(setOf("x.com"), merged.domains)
    }

    @Test
    fun `expired lock is replaced, not merged`() {
        val expired = AlarmDetoxLock(until = 10, packages = setOf("old"), domains = emptySet(), label = "Old")
        val lock = WakeUpDetoxPolicy.lockFor(alarm(WakeUpDetox(enabled = true, apps = listOf("new"))), 1_000, expired)!!
        assertEquals(setOf("new"), lock.packages)
    }

    @Test
    fun `site input normalization`() {
        assertEquals("instagram.com", SiteRules.normalize("Instagram.com"))
        assertEquals("instagram.com", SiteRules.normalize("https://www.instagram.com/reels/abc?x=1"))
        assertEquals("news.ycombinator.com", SiteRules.normalize("news.ycombinator.com:443/item"))
        assertNull(SiteRules.normalize("instagram"))
        assertNull(SiteRules.normalize("not a site"))
        assertNull(SiteRules.normalize(""))
    }

    @Test
    fun `address bar host matches domain and subdomains only`() {
        val blocked = setOf("youtube.com")
        assertTrue(SiteRules.isBlocked(SiteRules.hostOf("m.youtube.com/shorts")!!, blocked))
        assertTrue(SiteRules.isBlocked(SiteRules.hostOf("youtube.com")!!, blocked))
        assertTrue(SiteRules.isBlocked(SiteRules.hostOf("music.youtube.com")!!, blocked))
        assertFalse(SiteRules.isBlocked(SiteRules.hostOf("notyoutube.com")!!, blocked))
        assertNull(SiteRules.hostOf("cats playing piano"))
    }

    @Test
    fun `rules engine blocks detox apps until the lock ends`() {
        val engine = RulesEngine()
        val zone = ZoneId.of("Europe/Belgrade")
        val state = RuntimeState(alarmDetoxUntil = 5_000, alarmDetoxPackages = setOf("com.instagram.android"), alarmDetoxLabel = "Work")
        val decision = engine.evaluate("com.instagram.android", 1_000, zone, RulesSnapshot.EMPTY, state)!!
        assertEquals(BlockReason.ALARM_DETOX, decision.reason)
        assertEquals(5_000, decision.blockedUntil)
        assertEquals("Wake-up detox is active · Work", decision.message)
        assertNull(engine.evaluate("com.whatsapp", 1_000, zone, RulesSnapshot.EMPTY, state))
        assertNull(engine.evaluate("com.instagram.android", 5_000, zone, RulesSnapshot.EMPTY, state))
    }

    @Test
    fun `emergency pause does not lift the detox`() {
        // Pauza je za Screen Manager limite; detox posle alarma ne sme da se zaobiđe jednim klikom.
        val engine = RulesEngine()
        val rules = RulesSnapshot(emergency = EmergencySessionConfig(defaultDurationMinutes = 15, activeUntilMillis = 10_000))
        val state = RuntimeState(alarmDetoxUntil = 50_000, alarmDetoxPackages = setOf("a"))
        assertEquals(BlockReason.ALARM_DETOX, engine.evaluate("a", 1_000, ZoneId.of("UTC"), rules, state)?.reason)
        assertNull("aplikacija van detox-a je puštena", engine.evaluate("b", 1_000, ZoneId.of("UTC"), rules, state))
    }
}
