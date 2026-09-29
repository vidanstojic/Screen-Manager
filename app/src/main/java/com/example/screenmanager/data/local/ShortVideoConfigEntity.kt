package com.example.screenmanager.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.screenmanager.model.ShortVideoConfig
import com.example.screenmanager.model.ShortsMode

/**
 * Globalno Shorts/Reels pravilo (jedan red).
 *
 * v6: dodati `mode` i parametri interval moda. Default vrednosti u
 * [ColumnInfo] moraju da se poklapaju sa `MIGRATION_5_6` u [ScreenManagerDatabase].
 */
@Entity(tableName = "short_video_configs")
data class ShortVideoConfigEntity(
    @PrimaryKey val id: String = "shorts_global",
    val maxReelsWatchMinutes: Int,
    val fullAppBlockMinutes: Int,
    val selectedAppIds: String, // comma-separated package names
    val isEnabled: Boolean,
    @ColumnInfo(defaultValue = "BUDGET") val mode: String = ShortsMode.BUDGET.name,
    @ColumnInfo(defaultValue = "5") val sessionLengthMinutes: Int = 5,
    @ColumnInfo(defaultValue = "3") val maxSessions: Int = 3,
    @ColumnInfo(defaultValue = "30") val cooldownMinutes: Int = 30
)

fun ShortVideoConfigEntity.toModel(): ShortVideoConfig {
    return ShortVideoConfig(
        maxReelsWatchMinutes = maxReelsWatchMinutes,
        fullAppBlockMinutes = fullAppBlockMinutes,
        selectedAppIds = selectedAppIds.split(",").filter { it.isNotEmpty() },
        isEnabled = isEnabled,
        mode = ShortsMode.entries.firstOrNull { it.name == mode } ?: ShortsMode.BUDGET,
        sessionLengthMinutes = sessionLengthMinutes,
        maxSessions = maxSessions,
        cooldownMinutes = cooldownMinutes
    )
}

fun ShortVideoConfig.toEntity(): ShortVideoConfigEntity {
    return ShortVideoConfigEntity(
        maxReelsWatchMinutes = maxReelsWatchMinutes,
        fullAppBlockMinutes = fullAppBlockMinutes,
        selectedAppIds = selectedAppIds.joinToString(","),
        isEnabled = isEnabled,
        mode = mode.name,
        sessionLengthMinutes = sessionLengthMinutes,
        maxSessions = maxSessions,
        cooldownMinutes = cooldownMinutes
    )
}
