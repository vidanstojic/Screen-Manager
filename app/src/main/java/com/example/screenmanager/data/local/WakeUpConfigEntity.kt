package com.example.screenmanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.screenmanager.model.WakeUpConfig

@Entity(tableName = "wake_up_configs")
data class WakeUpConfigEntity(
    @PrimaryKey val id: String = "wakeup_global",
    val inactivityHours: Int,
    val triggerDelayMinutes: Int,
    val blockDurationMinutes: Int,
    val selectedAppIds: String, // JSON string
    val isEnabled: Boolean
)

fun WakeUpConfigEntity.toModel(): WakeUpConfig {
    return WakeUpConfig(
        inactivityHours = inactivityHours,
        triggerDelayMinutes = triggerDelayMinutes,
        blockDurationMinutes = blockDurationMinutes,
        selectedAppIds = selectedAppIds.split(",").filter { it.isNotEmpty() },
        isEnabled = isEnabled
    )
}

fun WakeUpConfig.toEntity(): WakeUpConfigEntity {
    return WakeUpConfigEntity(
        inactivityHours = inactivityHours,
        triggerDelayMinutes = triggerDelayMinutes,
        blockDurationMinutes = blockDurationMinutes,
        selectedAppIds = selectedAppIds.joinToString(","),
        isEnabled = isEnabled
    )
}
