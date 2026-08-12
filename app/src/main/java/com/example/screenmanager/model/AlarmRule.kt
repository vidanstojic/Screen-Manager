package com.example.screenmanager.model

data class AlarmRule(
    val id: Long = System.currentTimeMillis(),
    val hour: Int,
    val minute: Int,
    val label: String,
    val repeatDays: Set<String>,
    val enabled: Boolean
)
