package com.example.screenmanager.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "app_usage_log",
    indices = [Index(value = ["packageName", "startTimeStamp", "endTimeStamp"], unique = true)]
)
data class AppUsageLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val startTimeStamp: Long,
    val endTimeStamp: Long,
    val durationMs: Long,
    val isShortForm: Boolean = false,
    val timestamp: LocalDateTime? = null
)
