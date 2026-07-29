package com.example.screenmanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.screenmanager.model.AppLimitRule

@Entity(tableName = "app_limit_rules")
data class AppLimitRuleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val selectedAppIds: String, // JSON string
    val dailyLimitMinutes: Int,
    val blockDurationMinutes: Int,
    val isEnabled: Boolean,
    val description: String
)

fun AppLimitRuleEntity.toModel(): AppLimitRule {
    return AppLimitRule(
        id = id,
        name = name,
        selectedAppIds = selectedAppIds.split(",").filter { it.isNotEmpty() },
        dailyLimitMinutes = dailyLimitMinutes,
        blockDurationMinutes = blockDurationMinutes,
        isEnabled = isEnabled,
        description = description
    )
}

fun AppLimitRule.toEntity(): AppLimitRuleEntity {
    return AppLimitRuleEntity(
        id = id,
        name = name,
        selectedAppIds = selectedAppIds.joinToString(","),
        dailyLimitMinutes = dailyLimitMinutes,
        blockDurationMinutes = blockDurationMinutes,
        isEnabled = isEnabled,
        description = description
    )
}
