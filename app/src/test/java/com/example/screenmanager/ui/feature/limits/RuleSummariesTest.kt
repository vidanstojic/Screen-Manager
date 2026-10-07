package com.example.screenmanager.ui.feature.limits

import com.example.screenmanager.model.AppLimitRule
import com.example.screenmanager.model.ScheduleRule
import com.example.screenmanager.model.SessionLimitRule
import com.example.screenmanager.model.ShortVideoConfig
import com.example.screenmanager.model.ShortsMode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class RuleSummariesTest {

    private val labels = mapOf("ig" to "Instagram", "yt" to "YouTube", "tt" to "TikTok", "wa" to "WhatsApp")
    private val labelFor: (String) -> String = { labels[it] ?: it }

    @Test
    fun `apps summary lists up to two names then counts the rest`() {
        assertEquals("No apps", appsSummary(emptyList(), labelFor))
        assertEquals("Instagram", appsSummary(listOf("ig"), labelFor))
        assertEquals("Instagram, YouTube", appsSummary(listOf("ig", "yt"), labelFor))
        assertEquals("Instagram, YouTube +2", appsSummary(listOf("ig", "yt", "tt", "wa"), labelFor))
    }

    @Test
    fun `unknown package falls back to its package name`() {
        assertEquals("com.unknown", appsSummary(listOf("com.unknown"), labelFor))
    }

    @Test
    fun `days summary names common groups`() {
        assertEquals("No days", daysSummary(emptySet()))
        assertEquals("Every day", daysSummary(DayOfWeek.entries.toSet()))
        assertEquals("Weekdays", daysSummary(DayOfWeek.entries.filter { it.value <= 5 }.toSet()))
        assertEquals("Weekends", daysSummary(setOf(DayOfWeek.SUNDAY, DayOfWeek.SATURDAY)))
        // Uvek u kalendarskom redosledu, bez obzira na redosled u skupu.
        assertEquals("Mon, Wed, Fri", daysSummary(setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)))
    }

    @Test
    fun `daily limit summary`() {
        val rule = AppLimitRule("1", "Social", listOf("ig", "yt"), dailyLimitMinutes = 90, blockDurationMinutes = 0)
        assertEquals("1h 30m per day · Instagram, YouTube", rule.summary(labelFor))
    }

    @Test
    fun `session limit summary omits the break when there is none`() {
        val rule = SessionLimitRule("1", "IG", listOf("ig"), sessionLengthMinutes = 5, maxSessions = 4, cooldownMinutes = 15)
        assertEquals("4 × 5m, 15m break · Instagram", rule.summary(labelFor))
        assertEquals("4 × 5m · Instagram", rule.copy(cooldownMinutes = 0).summary(labelFor))
    }

    @Test
    fun `schedule summary shows hours days and apps`() {
        val rule = ScheduleRule(
            id = "1",
            name = "Night",
            startTime = LocalTime.of(22, 0),
            endTime = LocalTime.of(7, 0),
            daysOfWeek = DayOfWeek.entries.toSet(),
            selectedAppIds = listOf("yt")
        )
        assertEquals("22:00–07:00 · Every day · YouTube", rule.summary(labelFor))
    }

    @Test
    fun `shorts summary depends on the mode`() {
        val config = ShortVideoConfig(sessionLengthMinutes = 5, maxSessions = 3, cooldownMinutes = 30)
        assertEquals("Always blocked", config.copy(mode = ShortsMode.BLOCKED).summary())
        assertEquals("3 × 5m, 30m break", config.copy(mode = ShortsMode.SESSIONS).summary())
        assertEquals(
            "15m per day, then the app is blocked for 1h",
            config.copy(mode = ShortsMode.BUDGET, maxReelsWatchMinutes = 15, fullAppBlockMinutes = 60).summary()
        )
    }
}
