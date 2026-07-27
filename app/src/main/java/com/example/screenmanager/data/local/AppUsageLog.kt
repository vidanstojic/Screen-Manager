package com.example.screenmanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage_log")
data class AppUsageLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val startTimeStamp: Long,
    val endTimeStamp: Long,
    val durationMs: Long
)
