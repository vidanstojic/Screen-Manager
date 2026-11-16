package com.example.screenmanager.domain

import com.example.screenmanager.service.AlarmReceiver
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmSchedulerTest {
    @Test
    fun `alarm receiver is addressed by its real class name`() {
        assertEquals(AlarmReceiver::class.java.name, AlarmScheduler.ALARM_RECEIVER_CLASS)
    }
}
