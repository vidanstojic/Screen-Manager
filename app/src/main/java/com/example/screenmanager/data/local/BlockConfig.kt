package com.example.screenmanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "block_config")
data class BlockConfig(
    @PrimaryKey val packageName: String,
    val isBlockedDuringWakeup: Boolean,
    val maxDailyAllowedMinutes: Int
)
