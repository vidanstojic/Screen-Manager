package com.example.screenmanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.screenmanager.model.AlarmRule

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey val id: Long,
    val hour: Int,
    val minute: Int,
    val label: String,
    val repeatDays: String,
    val enabled: Boolean
)

fun AlarmEntity.toModel(): AlarmRule {
    return AlarmRule(
        id = id,
        hour = hour,
        minute = minute,
        label = label,
        repeatDays = repeatDays.split(",").filter { it.isNotBlank() }.toSet(),
        enabled = enabled
    )
}

fun AlarmRule.toEntity(): AlarmEntity {
    return AlarmEntity(
        id = id,
        hour = hour,
        minute = minute,
        label = label,
        repeatDays = repeatDays.sorted().joinToString(","),
        enabled = enabled
    )
}
