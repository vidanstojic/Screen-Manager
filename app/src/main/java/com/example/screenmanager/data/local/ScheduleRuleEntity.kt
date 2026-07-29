package com.example.screenmanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.screenmanager.model.ScheduleRule
import java.time.DayOfWeek
import java.time.LocalTime

@Entity(tableName = "schedule_rules")
data class ScheduleRuleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val startTimeHour: Int,
    val startTimeMinute: Int,
    val endTimeHour: Int,
    val endTimeMinute: Int,
    val daysOfWeek: String, // comma-separated day names
    val selectedAppIds: String, // JSON string
    val isEnabled: Boolean
)

fun ScheduleRuleEntity.toModel(): ScheduleRule {
    return ScheduleRule(
        id = id,
        name = name,
        startTime = LocalTime.of(startTimeHour, startTimeMinute),
        endTime = LocalTime.of(endTimeHour, endTimeMinute),
        daysOfWeek = daysOfWeek.split(",").mapNotNull { day ->
            try {
                DayOfWeek.valueOf(day)
            } catch (e: Exception) {
                null
            }
        }.toSet(),
        selectedAppIds = selectedAppIds.split(",").filter { it.isNotEmpty() },
        isEnabled = isEnabled
    )
}

fun ScheduleRule.toEntity(): ScheduleRuleEntity {
    return ScheduleRuleEntity(
        id = id,
        name = name,
        startTimeHour = startTime.hour,
        startTimeMinute = startTime.minute,
        endTimeHour = endTime.hour,
        endTimeMinute = endTime.minute,
        daysOfWeek = daysOfWeek.joinToString(",") { it.name },
        selectedAppIds = selectedAppIds.joinToString(","),
        isEnabled = isEnabled
    )
}
