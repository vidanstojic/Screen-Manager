package com.example.screenmanager.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitorHealthPolicyTest {
    private val boot = 1_000L

    @Test
    fun `service found stopped after it was running is an unexpected stop`() {
        assertTrue(MonitorHealthPolicy.isUnexpectedStop(serviceRunning = false, heartbeatAt = 5_000, lastReportedAt = 0, bootAt = boot))
    }

    @Test
    fun `running service is never reported`() {
        assertFalse(MonitorHealthPolicy.isUnexpectedStop(serviceRunning = true, heartbeatAt = 5_000, lastReportedAt = 0, bootAt = boot))
    }

    @Test
    fun `first launch and clean shutdown have no heartbeat`() {
        assertFalse(MonitorHealthPolicy.isUnexpectedStop(serviceRunning = false, heartbeatAt = 0, lastReportedAt = 0, bootAt = boot))
    }

    @Test
    fun `heartbeat from before a reboot is not a kill`() {
        assertFalse(MonitorHealthPolicy.isUnexpectedStop(serviceRunning = false, heartbeatAt = 500, lastReportedAt = 0, bootAt = boot))
    }

    @Test
    fun `the same stop is reported once and a later one again`() {
        assertFalse(MonitorHealthPolicy.isUnexpectedStop(serviceRunning = false, heartbeatAt = 5_000, lastReportedAt = 6_000, bootAt = boot))
        assertTrue(MonitorHealthPolicy.isUnexpectedStop(serviceRunning = false, heartbeatAt = 9_000, lastReportedAt = 6_000, bootAt = boot))
    }
}
