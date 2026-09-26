package com.example.screenmanager.data.local

import androidx.room.Entity
import androidx.room.Index

/**
 * Satni rollup potrošnje: jedan red = (paket, lokalni dan, lokalni sat).
 *
 * Sve UI projekcije (dan/nedelja/mesec) čitaju odavde, pa nema skupog
 * preračunavanja u realnom vremenu (TRS 2.1). [epochDay] je
 * `LocalDate.toEpochDay()` u zoni uređaja, [hour] je 0..23.
 */
@Entity(
    tableName = "usage_hourly",
    primaryKeys = ["packageName", "epochDay", "hour"],
    indices = [Index(value = ["epochDay"])]
)
data class UsageHourlyEntity(
    val packageName: String,
    val epochDay: Long,
    val hour: Int,
    val durationMs: Long,
    val sessionStarts: Int
)
