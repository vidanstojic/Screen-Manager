package com.example.screenmanager.data.repository

import com.example.screenmanager.domain.wakeup.WakeUpLockoutTrigger
import com.example.screenmanager.domain.wakeup.WakeUpSource

/**
 * Implementacija [WakeUpLockoutTrigger]: čita korisnikov WakeUpConfig i
 * upisuje kraj blokade u runtime stanje; RulesEngine ga zatim primenjuje.
 */
class WakeUpLockoutController(
    private val settingsRepository: SettingsRepository,
    private val runtimeStateRepository: RuntimeStateRepository
) : WakeUpLockoutTrigger {

    override suspend fun trigger(source: WakeUpSource, now: Long): Long? {
        val config = settingsRepository.getWakeUpConfig() ?: return null
        if (!config.isEnabled || config.selectedAppIds.isEmpty()) return null
        val until = now + config.blockDurationMinutes.coerceAtLeast(1) * 60_000L
        // Ne skraćujemo već aktivnu dužu blokadu.
        if (runtimeStateRepository.wakeUpBlockedUntil() >= until) return runtimeStateRepository.wakeUpBlockedUntil()
        runtimeStateRepository.setWakeUpBlockedUntil(until)
        return until
    }
}
