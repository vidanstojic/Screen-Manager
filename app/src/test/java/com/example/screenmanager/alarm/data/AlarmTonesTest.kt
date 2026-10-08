package com.example.screenmanager.alarm.data

import com.example.screenmanager.model.AlarmSound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmTonesTest {

    @Test
    fun `default tone is a built-in sound`() {
        assertTrue(AlarmTones.builtins.any { it.id == AlarmSound.DEFAULT_TONE_ID })
        assertEquals("Sunrise", AlarmTones.displayName(AlarmSound()))
    }

    @Test
    fun `built-in ids are unique and prefixed`() {
        val ids = AlarmTones.builtins.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it.startsWith(AlarmTones.BUILTIN_PREFIX) })
    }

    @Test
    fun `display names for system and custom tones`() {
        assertEquals("System default", AlarmTones.displayName(AlarmSound(toneId = AlarmTones.SYSTEM_ID)))
        assertEquals("wake up song", AlarmTones.displayName(AlarmSound(toneId = "custom:content://x/1", customName = "wake up song")))
        assertEquals("Your sound", AlarmTones.displayName(AlarmSound(toneId = "custom:content://x/1")))
    }

    @Test
    fun `unknown built-in falls back to the first sound`() {
        assertEquals("Sunrise", AlarmTones.displayName(AlarmSound(toneId = "builtin:removed")))
    }
}
