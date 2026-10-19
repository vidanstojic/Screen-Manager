package com.example.screenmanager.domain.wakeup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class WakeUpDetectorTest {
    private val zone = ZoneId.of("Europe/Belgrade")
    private val hours = 5
    private val delay = 2

    private fun at(time: String) = LocalDateTime.parse("2026-10-15T$time").atZone(zone).toInstant().toEpochMilli()
    private fun prevDay(time: String) = LocalDateTime.parse("2026-10-14T$time").atZone(zone).toInstant().toEpochMilli()

    /** Upaljen ekran u [from, to). */
    private fun WakeState.use(from: Long, to: Long) =
        WakeUpDetector.onScreenOff(WakeUpDetector.onScreenOn(this, from, hours, zone), to)

    /** Večernje korišćenje do 23:00 → noć počinje u 23:00. */
    private val evening = WakeState().use(prevDay("22:30:00"), prevDay("23:00:00"))

    @Test
    fun `night starts when real use ends`() {
        assertEquals(prevDay("23:00:00"), evening.darkSince)
    }

    @Test
    fun `morning use after a long night triggers the lock`() {
        val on = WakeUpDetector.onScreenOn(evening, at("07:00:00"), hours, zone)
        assertEquals(at("07:00:00"), on.wakeAt)
        assertFalse(WakeUpDetector.shouldTrigger(on, at("07:01:00"), delay))
        assertTrue(WakeUpDetector.shouldTrigger(on, at("07:02:00"), delay))
    }

    @Test
    fun `glances at night do not move the start of the night`() {
        // BUG-004: pogled na sat u 03:00 je ranije pomerao noć na 03:00:10 → u 07:00 samo 4 h.
        val glanced = evening.use(at("03:00:00"), at("03:00:10"))
        assertEquals(prevDay("23:00:00"), glanced.darkSince)
        val morning = WakeUpDetector.onScreenOn(glanced, at("07:00:00"), hours, zone)
        assertTrue(WakeUpDetector.shouldTrigger(morning, at("07:02:00"), delay))
    }

    @Test
    fun `screen time adds up across several short wake-ups`() {
        // BUG-004: alarm/pogled 30 s pa zaključan telefon ranije je trošio celo jutro.
        val first = evening.use(at("07:00:00"), at("07:00:30"))
        assertEquals(30_000L, first.wakeOnMs)
        val second = WakeUpDetector.onScreenOn(first, at("07:10:00"), hours, zone)
        assertEquals(90_000L, WakeUpDetector.remainingMs(second, at("07:10:00"), delay))
        assertFalse(WakeUpDetector.shouldTrigger(second, at("07:11:00"), delay))
        assertTrue(WakeUpDetector.shouldTrigger(second, at("07:11:30"), delay))
    }

    @Test
    fun `short night does not trigger`() {
        val on = WakeUpDetector.onScreenOn(evening, at("03:30:00"), hours, zone)
        assertNull(on.wakeAt)
        assertNull(WakeUpDetector.remainingMs(on, at("03:35:00"), delay))
    }

    @Test
    fun `stale candidate is replaced by a later wake-up after the same night`() {
        // Pogled u 04:30 (5,5 h noći) postaje kandidat; u 08:00 je zastareo, ali noć je i dalje od 23:00.
        val early = evening.use(at("04:30:00"), at("04:30:20"))
        assertEquals(at("04:30:00"), early.wakeAt)
        val morning = WakeUpDetector.onScreenOn(early, at("08:00:00"), hours, zone)
        assertEquals(at("08:00:00"), morning.wakeAt)
        assertEquals(0L, morning.wakeOnMs)
        assertTrue(WakeUpDetector.shouldTrigger(morning, at("08:02:00"), delay))
    }

    @Test
    fun `after the lock fires the next night starts at the next screen off`() {
        val on = WakeUpDetector.onScreenOn(evening, at("07:00:00"), hours, zone)
        val fired = WakeUpDetector.onTriggered(on)
        assertNull(fired.wakeAt)
        val off = WakeUpDetector.onScreenOff(fired, at("07:20:00"))
        assertEquals(at("07:20:00"), off.darkSince)
        // Dnevno paljenje ekrana ne pravi novog kandidata.
        assertNull(WakeUpDetector.onScreenOn(off, at("09:00:00"), hours, zone).wakeAt)
    }

    @Test
    fun `zero delay locks as soon as the screen turns on`() {
        val on = WakeUpDetector.onScreenOn(evening, at("07:00:00"), hours, zone)
        assertTrue(WakeUpDetector.shouldTrigger(on, at("07:00:00"), 0))
    }

    @Test
    fun `daytime real use moves the night forward`() {
        val day = evening.use(at("07:00:00"), at("07:00:30")).let(WakeUpDetector::onTriggered)
            .use(at("12:00:00"), at("12:05:00"))
        assertEquals(at("12:05:00"), day.darkSince)
        // Kratak pogled u 14:00 ne pomera noć.
        assertEquals(at("12:05:00"), day.use(at("14:00:00"), at("14:00:20")).darkSince)
    }

    @Test
    fun `a long daytime break is not a night`() {
        // Bez telefona 09:00–16:00 (7 h > 5 h): ranije je to palilo "jutarnju" blokadu.
        val day = evening.use(at("07:00:00"), at("07:00:30")).let(WakeUpDetector::onTriggered)
            .use(at("08:30:00"), at("09:00:00"))
        assertNull(WakeUpDetector.onScreenOn(day, at("16:00:00"), hours, zone).wakeAt)
    }

    @Test
    fun `an evening break is not a night`() {
        val afternoon = WakeState().use(at("17:00:00"), at("17:30:00"))
        assertNull(WakeUpDetector.onScreenOn(afternoon, at("23:00:00"), hours, zone).wakeAt)
    }

    @Test
    fun `late sleeper still gets the morning lock`() {
        // Spava 04:00–11:00: u noćni prozor pada 4 h.
        val late = WakeState().use(at("03:30:00"), at("04:00:00"))
        val on = WakeUpDetector.onScreenOn(late, at("11:00:00"), hours, zone)
        assertEquals(at("11:00:00"), on.wakeAt)
    }

    @Test
    fun `night overlap is counted across midnight and days`() {
        assertEquals(7 * 3_600_000L, WakeUpDetector.nightOverlapMs(prevDay("23:00:00"), at("07:00:00"), zone))
        assertEquals(0L, WakeUpDetector.nightOverlapMs(at("09:00:00"), at("16:00:00"), zone))
        assertEquals(WakeUpDetector.MIN_NIGHT_OVERLAP_MS, WakeUpDetector.nightOverlapMs(prevDay("09:00:00"), at("10:00:00"), zone))
    }
}
