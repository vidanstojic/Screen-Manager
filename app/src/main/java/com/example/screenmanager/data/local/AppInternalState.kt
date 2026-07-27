package com.example.screenmanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_internal_state")
data class AppInternalState(
    @PrimaryKey val stateKey: String,
    val stateValueLong: Long
)
