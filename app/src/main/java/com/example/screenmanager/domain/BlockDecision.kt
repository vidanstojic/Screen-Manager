package com.example.screenmanager.domain

data class BlockDecision(
    val packageName: String,
    val reason: String,
    val blockedUntil: Long
) {
    fun remainingMs(now: Long = System.currentTimeMillis()): Long = (blockedUntil - now).coerceAtLeast(0)
}
