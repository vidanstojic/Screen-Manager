package com.example.screenmanager.ui.model

import com.example.screenmanager.data.local.DailyUsage
import com.example.screenmanager.data.local.HourlyUsage
import com.example.screenmanager.domain.TimeBuckets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageUiModelsTest {

    @Test
    fun `hourly list always has 24 slots and fills gaps with zero`() {
        val hours = listOf(HourlyUsage(hour = 9, durationMs = 600_000), HourlyUsage(hour = 23, durationMs = 60_000)).toHourlyMs()
        assertEquals(24, hours.size)
        assertEquals(600_000L, hours[9])
        assertEquals(60_000L, hours[23])
        assertEquals(660_000L, hours.sum())
    }

    @Test
    fun `daily list has the requested number of days`() {
        val days = listOf(DailyUsage(day = 0, durationMs = 1_000), DailyUsage(day = 6, durationMs = 2_000)).toDailyMs(7)
        assertEquals(listOf(1_000L, 0L, 0L, 0L, 0L, 0L, 2_000L), days)
        assertEquals(31, emptyList<DailyUsage>().toDailyMs(31).size)
    }

    @Test
    fun `last seven days are consecutive midnights ending today`() {
        val now = System.currentTimeMillis()
        val days = lastSevenDays(now)
        assertEquals(7, days.size)
        assertTrue(days.last().isToday)
        assertEquals(1, days.count { it.isToday })
        assertEquals(TimeBuckets.startOfToday(now), days.last().dayStart)
        // Svaki sledeći dan je tačno jedan kalendarski dan posle prethodnog.
        days.zipWithNext { a, b ->
            assertEquals(TimeBuckets.epochDay(a.dayStart) + 1, TimeBuckets.epochDay(b.dayStart))
        }
    }
}
