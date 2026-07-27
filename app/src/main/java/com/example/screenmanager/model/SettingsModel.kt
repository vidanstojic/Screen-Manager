package com.example.screenmanager.model

import java.time.DayOfWeek
import java.time.LocalTime

// 1. Detekcija buđenja (Morning Routine)
data class WakeUpConfig(
    val inactivityHours: Int = 7,             // Koliko sati neaktivnosti pali flag
    val triggerDelayMinutes: Int = 2,          // Korišćenje duže od 2 min aktivira mod
    val blockDurationMinutes: Int = 30,        // Koliko dugo su aplikacije blokirane
    val selectedAppIds: List<String> = emptyList(), // Lista ID-eva izabranih aplikacija
    val isEnabled: Boolean = true
)

// 2. Reels & Shorts limit
data class ShortVideoConfig(
    val maxReelsWatchMinutes: Int = 15,        // Maksimalno trajanje gledanja reels/shorts
    val fullAppBlockMinutes: Int = 60,         // Dužina blokade cele aplikacije
    val selectedAppIds: List<String> = listOf("instagram", "youtube"),
    val isEnabled: Boolean = true
)

// 3. Zakazano blokiranje (Scheduled Block)
data class ScheduleRule(
    val id: String,
    val name: String,                          // npr. "Noćni mir", "Radno vreme"
    val startTime: LocalTime,                  // npr. 22:00
    val endTime: LocalTime,                    // npr. 07:00
    val daysOfWeek: Set<DayOfWeek>,            // Dani u nedelji
    val selectedAppIds: List<String> = emptyList(),
    val isEnabled: Boolean = true
)