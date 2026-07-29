package com.example.screenmanager.domain

import com.example.screenmanager.data.repository.EmergencySessionConfigRepository
import com.example.screenmanager.model.EmergencySessionConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EmergencySessionMonitor(
    private val repository: EmergencySessionConfigRepository
) {
    fun observeSessionStatus(): Flow<SessionStatus> {
        return repository.observe()
            .map { config ->
                if (config == null) {
                    SessionStatus.Inactive
                } else if (!config.isActive) {
                    SessionStatus.Inactive
                } else {
                    // Try to parse the activeUntilLabel to determine if it's expired
                    val isExpired = checkIfExpired(config.activeUntilLabel)
                    if (isExpired) {
                        // Auto-deactivate
                        repository.update(config.copy(isActive = false, activeUntilLabel = null))
                        SessionStatus.Inactive
                    } else {
                        SessionStatus.Active(config.activeUntilLabel ?: "Unknown")
                    }
                }
            }
            .distinctUntilChanged()
    }

    suspend fun activateSession(durationMinutes: Int) {
        val config = repository.get() ?: EmergencySessionConfig()
        val activeUntil = LocalDateTime.now().plusMinutes(durationMinutes.toLong())
        
        val updated = config.copy(
            isActive = true,
            activeUntilLabel = formatTime(activeUntil),
            defaultDurationMinutes = durationMinutes
        )
        repository.update(updated)
    }

    suspend fun deactivateSession() {
        val config = repository.get() ?: EmergencySessionConfig()
        val updated = config.copy(
            isActive = false,
            activeUntilLabel = null
        )
        repository.update(updated)
    }

    private fun checkIfExpired(timeLabel: String?): Boolean {
        if (timeLabel == null || timeLabel.isEmpty()) return false
        
        return try {
            // Assuming format like "15:45" (HH:mm) or timestamp string
            val parts = timeLabel.split(":")
            if (parts.size >= 2) {
                val hour = parts[0].toIntOrNull() ?: return false
                val minute = parts[1].substringBefore(" ").toIntOrNull() ?: return false
                
                val scheduledTime = LocalDateTime.now().withHour(hour).withMinute(minute)
                LocalDateTime.now().isAfter(scheduledTime)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun formatTime(dateTime: LocalDateTime): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        return dateTime.format(formatter)
    }
}

sealed class SessionStatus {
    object Inactive : SessionStatus()
    data class Active(val expireTime: String) : SessionStatus()
}
