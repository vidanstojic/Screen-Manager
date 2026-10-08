package com.example.screenmanager.data.local

import com.example.screenmanager.model.AlarmMission
import com.example.screenmanager.model.AlarmRule
import com.example.screenmanager.model.AlarmSound
import com.example.screenmanager.model.MissionDifficulty
import com.example.screenmanager.model.MissionType
import com.example.screenmanager.model.SnoozeConfig
import com.example.screenmanager.model.WakeCheckConfig
import com.example.screenmanager.model.WakeUpDetox
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class AlarmEntityMappingTest {

    private val full = AlarmRule(
        id = 42,
        hour = 6,
        minute = 45,
        label = "Gym",
        repeatDays = setOf("Mon", "Wed"),
        enabled = true,
        sound = AlarmSound(toneId = "custom:content://media/1", customName = "song.mp3", volumePercent = 60, fadeInSeconds = 0, vibrate = false),
        snooze = SnoozeConfig(enabled = false, minutes = 10, maxCount = 1),
        mission = AlarmMission(MissionType.TYPING, MissionDifficulty.HARD, count = 2, steps = 50, customText = "I am awake"),
        detox = WakeUpDetox(enabled = true, apps = listOf("com.instagram.android"), sites = listOf("youtube.com", "x.com"), minutes = 45),
        wakeCheck = WakeCheckConfig(enabled = true, delayMinutes = 10)
    )

    @Test
    fun `entity round trip keeps every setting`() {
        assertEquals(full, full.toEntity().toModel())
    }

    @Test
    fun `row from before v7 maps to an alarm with default options`() {
        val legacy = AlarmEntity(id = 1, hour = 7, minute = 0, label = "", repeatDays = "", enabled = true)
        val model = legacy.toModel()
        assertEquals(AlarmSound(), model.sound)
        assertEquals(AlarmMission(), model.mission)
        assertEquals(WakeUpDetox(), model.detox)
        assertEquals(WakeCheckConfig(), model.wakeCheck)
        assertEquals(emptySet<String>(), model.repeatDays)
    }

    @Test
    fun `unknown enum names fall back to defaults`() {
        val model = full.toEntity().copy(missionType = "LASER", missionDifficulty = "?").toModel()
        assertEquals(MissionType.NONE, model.mission.type)
        assertEquals(MissionDifficulty.MEDIUM, model.mission.difficulty)
    }

    @Test
    fun `alarm draft survives serialization`() {
        // Forma drži radnu kopiju u rememberSaveable (Bundle → Serializable).
        val fromDb = full.toEntity().toModel()
        val bytes = ByteArrayOutputStream().also { ObjectOutputStream(it).writeObject(fromDb) }.toByteArray()
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).readObject()
        assertEquals(fromDb, restored)
    }
}
