package com.example.screenmanager.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Baza više ne briše podatke kad migracija nedostaje (osim za verzije < 5),
 * pa svaki korak od prve čuvane verzije do trenutne mora imati migraciju.
 */
class MigrationCoverageTest {
    @Test
    fun `every version from 5 to the current one has a migration`() {
        val firstKept = ScreenManagerDatabase.DESTRUCTIVE_FROM.max() + 1
        val steps = ScreenManagerDatabase.MIGRATIONS.map { it.startVersion to it.endVersion }
        val expected = (firstKept until ScreenManagerDatabase.VERSION).map { it to it + 1 }
        assertEquals(expected, steps)
    }
}
