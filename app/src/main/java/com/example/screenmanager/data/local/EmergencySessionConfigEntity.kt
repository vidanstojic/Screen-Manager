package com.example.screenmanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.screenmanager.model.EmergencySessionConfig

@Entity(tableName = "emergency_sessions")
data class EmergencySessionConfigEntity(
    @PrimaryKey val id: String = "emergency_global",
    val defaultDurationMinutes: Int,
    val activeUntilLabel: String?,
    val manualEndEnabled: Boolean,
    val isActive: Boolean
)

fun EmergencySessionConfigEntity.toModel(): EmergencySessionConfig {
    return EmergencySessionConfig(
        defaultDurationMinutes = defaultDurationMinutes,
        activeUntilLabel = activeUntilLabel,
        manualEndEnabled = manualEndEnabled,
        isActive = isActive
    )
}

fun EmergencySessionConfig.toEntity(): EmergencySessionConfigEntity {
    return EmergencySessionConfigEntity(
        defaultDurationMinutes = defaultDurationMinutes,
        activeUntilLabel = activeUntilLabel,
        manualEndEnabled = manualEndEnabled,
        isActive = isActive
    )
}
