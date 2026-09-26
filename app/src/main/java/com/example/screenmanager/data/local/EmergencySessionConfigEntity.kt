package com.example.screenmanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.screenmanager.model.EmergencySessionConfig

/**
 * Emergency sesija se čuva kao apsolutni trenutak isteka ([activeUntilMillis]),
 * umesto ranijeg para `isActive` + "HH:mm" labela koji je pucao preko ponoći (B9).
 */
@Entity(tableName = "emergency_sessions")
data class EmergencySessionConfigEntity(
    @PrimaryKey val id: String = "emergency_global",
    val defaultDurationMinutes: Int,
    val activeUntilMillis: Long?,
    val manualEndEnabled: Boolean
)

fun EmergencySessionConfigEntity.toModel(): EmergencySessionConfig {
    return EmergencySessionConfig(
        defaultDurationMinutes = defaultDurationMinutes,
        activeUntilMillis = activeUntilMillis,
        manualEndEnabled = manualEndEnabled
    )
}

fun EmergencySessionConfig.toEntity(): EmergencySessionConfigEntity {
    return EmergencySessionConfigEntity(
        defaultDurationMinutes = defaultDurationMinutes,
        activeUntilMillis = activeUntilMillis,
        manualEndEnabled = manualEndEnabled
    )
}
