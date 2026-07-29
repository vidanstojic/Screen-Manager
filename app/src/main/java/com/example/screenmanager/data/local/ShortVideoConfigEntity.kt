package com.example.screenmanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.screenmanager.model.ShortVideoConfig

@Entity(tableName = "short_video_configs")
data class ShortVideoConfigEntity(
    @PrimaryKey val id: String = "shorts_global",
    val maxReelsWatchMinutes: Int,
    val fullAppBlockMinutes: Int,
    val selectedAppIds: String, // JSON string
    val isEnabled: Boolean
)

fun ShortVideoConfigEntity.toModel(): ShortVideoConfig {
    return ShortVideoConfig(
        maxReelsWatchMinutes = maxReelsWatchMinutes,
        fullAppBlockMinutes = fullAppBlockMinutes,
        selectedAppIds = selectedAppIds.split(",").filter { it.isNotEmpty() },
        isEnabled = isEnabled
    )
}

fun ShortVideoConfig.toEntity(): ShortVideoConfigEntity {
    return ShortVideoConfigEntity(
        maxReelsWatchMinutes = maxReelsWatchMinutes,
        fullAppBlockMinutes = fullAppBlockMinutes,
        selectedAppIds = selectedAppIds.joinToString(","),
        isEnabled = isEnabled
    )
}
