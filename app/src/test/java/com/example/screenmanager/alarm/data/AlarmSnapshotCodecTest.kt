package com.example.screenmanager.alarm.data

import com.example.screenmanager.model.AlarmMission
import com.example.screenmanager.model.AlarmRule
import com.example.screenmanager.model.AlarmSound
import com.example.screenmanager.model.MissionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmSnapshotCodecTest {
    @Test
    fun `alarm survives a round trip with all settings`() {
        val alarm = AlarmRule(
            id = 42L,
            hour = 6,
            minute = 45,
            label = "Work",
            repeatDays = setOf("MON", "TUE"),
            enabled = true,
            sound = AlarmSound(toneId = "builtin:marimba", volumePercent = 70, fadeInSeconds = 0),
            mission = AlarmMission(type = MissionType.STEPS)
        )
        val encoded = AlarmSnapshotCodec.encode(alarm)!!
        assertEquals(alarm, AlarmSnapshotCodec.decode(encoded))
    }

    @Test
    fun `corrupt snapshot decodes to null instead of crashing`() {
        assertNull(AlarmSnapshotCodec.decode("bm90IGFuIGFsYXJt"))
        assertNull(AlarmSnapshotCodec.decode("%%%"))
    }
}
