package com.example.screenmanager.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Sirova foreground sesija rekonstruisana iz UsageEvents.
 *
 * Jedinstvenost (packageName, startTimeStamp) je ključna: sync čita
 * događaje sa preklapanjem, a sesija koja je bila "otvorena" pri prošlom
 * sync-u se ponovo upisuje sa dužim krajem (REPLACE) umesto da se duplira
 * (B1/B4). Čuva se [com.example.screenmanager.domain.TimeBuckets.RAW_LOG_RETENTION_DAYS] dana.
 */
@Entity(
    tableName = "app_usage_log",
    indices = [
        Index(value = ["packageName", "startTimeStamp"], unique = true),
        Index(value = ["endTimeStamp"])
    ]
)
data class AppUsageLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val startTimeStamp: Long,
    val endTimeStamp: Long,
    val durationMs: Long
)
