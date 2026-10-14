package com.example.screenmanager.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionStateTest {
    private val all = PermissionState(
        hasUsageAccess = true,
        canDrawOverlay = true,
        accessibilityEnabled = true,
        notificationsEnabled = true,
        batteryUnrestricted = true
    )

    @Test
    fun `battery optimization keeps setup incomplete but not the critical permissions`() {
        val optimized = all.copy(batteryUnrestricted = false)
        assertTrue(optimized.allCriticalGranted)
        assertFalse(optimized.setupComplete)
        assertEquals(1, optimized.missingSetupSteps)
    }

    @Test
    fun `missing steps count every setup item`() {
        val none = PermissionState(false, false, false, false, batteryUnrestricted = false)
        assertEquals(4, none.missingSetupSteps)
        assertTrue(all.setupComplete)
        assertEquals(0, all.missingSetupSteps)
    }
}
