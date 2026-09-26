package com.example.screenmanager.domain.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionReconstructorTest {
    private val launcher = "com.launcher"
    private val reconstructor = SessionReconstructor(excludedPackages = setOf(launcher))

    private fun resumed(t: Long, p: String) = RawUsageEvent(t, p, RawEventKind.RESUMED)
    private fun paused(t: Long, p: String) = RawUsageEvent(t, p, RawEventKind.PAUSED)
    private fun screenOff(t: Long) = RawUsageEvent(t, null, RawEventKind.SCREEN_OFF)

    @Test
    fun `closed session is reconstructed`() {
        val result = reconstructor.reconstruct(listOf(resumed(1_000, "a"), paused(61_000, "a")), to = 100_000)
        assertEquals(listOf(UsageSession("a", 1_000, 61_000)), result.closedSessions)
        assertNull(result.openSession)
        assertEquals(100_000, result.safeCursor)
    }

    @Test
    fun `open session is reported and cursor stays at its start`() {
        val result = reconstructor.reconstruct(listOf(resumed(10_000, "a")), to = 70_000)
        assertEquals(UsageSession("a", 10_000, 70_000), result.openSession)
        assertEquals(10_000, result.safeCursor)
    }

    @Test
    fun `resuming another app closes the previous one`() {
        val result = reconstructor.reconstruct(listOf(resumed(0, "a"), resumed(30_000, "b"), paused(50_000, "b")), 60_000)
        assertEquals(
            listOf(UsageSession("a", 0, 30_000), UsageSession("b", 30_000, 50_000)),
            result.closedSessions
        )
    }

    @Test
    fun `activity switch inside the same app is merged`() {
        val events = listOf(resumed(0, "a"), paused(10_000, "a"), resumed(10_500, "a"), paused(40_000, "a"))
        val result = reconstructor.reconstruct(events, 60_000)
        assertEquals(listOf(UsageSession("a", 0, 40_000)), result.closedSessions)
    }

    @Test
    fun `launcher is excluded but closes previous session`() {
        val events = listOf(resumed(0, "a"), resumed(20_000, launcher), resumed(90_000, "b"))
        val result = reconstructor.reconstruct(events, 100_000)
        assertEquals(listOf(UsageSession("a", 0, 20_000)), result.closedSessions)
        assertEquals(UsageSession("b", 90_000, 100_000), result.openSession)
    }

    @Test
    fun `screen off closes everything and very short sessions are dropped`() {
        val events = listOf(resumed(0, "a"), screenOff(500), resumed(10_000, "b"), screenOff(70_000))
        val result = reconstructor.reconstruct(events, 100_000)
        assertEquals(listOf(UsageSession("b", 10_000, 70_000)), result.closedSessions)
    }

    @Test
    fun `re-reading from safe cursor is idempotent`() {
        val all = listOf(resumed(0, "a"), paused(30_000, "a"), resumed(40_000, "b"), paused(90_000, "b"))
        val first = reconstructor.reconstruct(all.filter { it.timestamp <= 60_000 }, 60_000)
        val second = reconstructor.reconstruct(all.filter { it.timestamp >= first.safeCursor }, 120_000)
        assertEquals(UsageSession("b", 40_000, 60_000), first.openSession)
        assertEquals(listOf(UsageSession("b", 40_000, 90_000)), second.closedSessions)
    }

    @Test
    fun `foreground resolver keeps state without new events`() {
        val resolver = ForegroundResolver()
        resolver.apply(resumed(0, "a"))
        assertEquals("a", resolver.currentPackage)
        resolver.apply(paused(10, "b"))
        assertEquals("a", resolver.currentPackage)
        resolver.apply(screenOff(20))
        assertNull(resolver.currentPackage)
    }
}
