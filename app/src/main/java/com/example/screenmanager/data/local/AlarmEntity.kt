package com.example.screenmanager.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.screenmanager.model.AlarmMission
import com.example.screenmanager.model.AlarmRule
import com.example.screenmanager.model.AlarmSound
import com.example.screenmanager.model.MissionDifficulty
import com.example.screenmanager.model.MissionType
import com.example.screenmanager.model.SnoozeConfig
import com.example.screenmanager.model.WakeCheckConfig
import com.example.screenmanager.model.WakeUpDetox

/**
 * Red tabele `alarms`. Kolone posle `enabled` su dodate u v7 (Phase 2):
 * njihove `defaultValue` vrednosti moraju biti iste kao u
 * [ScreenManagerDatabase.MIGRATION_6_7], inače Room odbija migraciju.
 */
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey val id: Long,
    val hour: Int,
    val minute: Int,
    val label: String,
    val repeatDays: String,
    val enabled: Boolean,
    // --- zvuk ---
    @ColumnInfo(defaultValue = AlarmSound.DEFAULT_TONE_ID) val soundTone: String = AlarmSound.DEFAULT_TONE_ID,
    val soundName: String? = null,
    @ColumnInfo(defaultValue = "80") val soundVolume: Int = 80,
    @ColumnInfo(defaultValue = "30") val soundFadeInSeconds: Int = 30,
    @ColumnInfo(defaultValue = "1") val vibrate: Boolean = true,
    // --- odlaganje ---
    @ColumnInfo(defaultValue = "1") val snoozeEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "5") val snoozeMinutes: Int = 5,
    @ColumnInfo(defaultValue = "3") val snoozeMaxCount: Int = 3,
    // --- misija ---
    @ColumnInfo(defaultValue = "NONE") val missionType: String = MissionType.NONE.name,
    @ColumnInfo(defaultValue = "MEDIUM") val missionDifficulty: String = MissionDifficulty.MEDIUM.name,
    @ColumnInfo(defaultValue = "3") val missionCount: Int = 3,
    @ColumnInfo(defaultValue = "30") val missionSteps: Int = 30,
    @ColumnInfo(defaultValue = "") val missionText: String = "",
    // --- wake-up detox ---
    @ColumnInfo(defaultValue = "0") val detoxEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "") val detoxApps: String = "",
    @ColumnInfo(defaultValue = "") val detoxSites: String = "",
    @ColumnInfo(defaultValue = "30") val detoxMinutes: Int = 30,
    // --- wake-up check ---
    @ColumnInfo(defaultValue = "0") val wakeCheckEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "5") val wakeCheckDelayMinutes: Int = 5
)

fun AlarmEntity.toModel(): AlarmRule {
    return AlarmRule(
        id = id,
        hour = hour,
        minute = minute,
        label = label,
        repeatDays = repeatDays.splitList().toSet(),
        enabled = enabled,
        sound = AlarmSound(
            toneId = soundTone,
            customName = soundName,
            volumePercent = soundVolume,
            fadeInSeconds = soundFadeInSeconds,
            vibrate = vibrate
        ),
        snooze = SnoozeConfig(enabled = snoozeEnabled, minutes = snoozeMinutes, maxCount = snoozeMaxCount),
        mission = AlarmMission(
            type = enumOr(missionType, MissionType.NONE),
            difficulty = enumOr(missionDifficulty, MissionDifficulty.MEDIUM),
            count = missionCount,
            steps = missionSteps,
            customText = missionText
        ),
        detox = WakeUpDetox(
            enabled = detoxEnabled,
            apps = detoxApps.splitList(),
            sites = detoxSites.splitList(),
            minutes = detoxMinutes
        ),
        wakeCheck = WakeCheckConfig(enabled = wakeCheckEnabled, delayMinutes = wakeCheckDelayMinutes)
    )
}

fun AlarmRule.toEntity(): AlarmEntity {
    return AlarmEntity(
        id = id,
        hour = hour,
        minute = minute,
        label = label,
        repeatDays = repeatDays.sorted().joinToString(","),
        enabled = enabled,
        soundTone = sound.toneId,
        soundName = sound.customName,
        soundVolume = sound.volumePercent,
        soundFadeInSeconds = sound.fadeInSeconds,
        vibrate = sound.vibrate,
        snoozeEnabled = snooze.enabled,
        snoozeMinutes = snooze.minutes,
        snoozeMaxCount = snooze.maxCount,
        missionType = mission.type.name,
        missionDifficulty = mission.difficulty.name,
        missionCount = mission.count,
        missionSteps = mission.steps,
        missionText = mission.customText,
        detoxEnabled = detox.enabled,
        detoxApps = detox.apps.joinToString(","),
        detoxSites = detox.sites.joinToString(","),
        detoxMinutes = detox.minutes,
        wakeCheckEnabled = wakeCheck.enabled,
        wakeCheckDelayMinutes = wakeCheck.delayMinutes
    )
}

private fun String.splitList(): List<String> = split(",").map { it.trim() }.filter { it.isNotEmpty() }

private inline fun <reified E : Enum<E>> enumOr(name: String, fallback: E): E =
    runCatching { enumValueOf<E>(name) }.getOrDefault(fallback)
