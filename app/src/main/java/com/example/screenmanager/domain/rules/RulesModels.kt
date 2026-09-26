package com.example.screenmanager.domain.rules

import com.example.screenmanager.model.AppLimitRule
import com.example.screenmanager.model.EmergencySessionConfig
import com.example.screenmanager.model.ScheduleRule
import com.example.screenmanager.model.SessionLimitRule
import com.example.screenmanager.model.ShortVideoConfig
import com.example.screenmanager.model.WakeUpConfig

/** Sva korisnička pravila u jednom nepromenljivom snimku. */
data class RulesSnapshot(
    val appLimits: List<AppLimitRule> = emptyList(),
    val schedules: List<ScheduleRule> = emptyList(),
    val sessionLimits: List<SessionLimitRule> = emptyList(),
    val shortVideo: ShortVideoConfig? = null,
    val wakeUp: WakeUpConfig? = null,
    val emergency: EmergencySessionConfig? = null
) {
    companion object {
        val EMPTY = RulesSnapshot()
    }
}

/**
 * Stanje interval-moda za jedno [SessionLimitRule] pravilo (po grupi).
 *
 * @property sessionStartedAt početak aktivne sesije, null ako sesija ne traje.
 * @property activeMs aktivno (foreground) vreme tekuće sesije.
 * @property lastSeenAt poslednji tick kada je aplikacija iz grupe bila u foreground-u.
 * @property frozenUntil kraj cool-down perioda (K), 0 ako nema.
 */
data class SessionState(
    val ruleId: String,
    val epochDay: Long,
    val sessionsUsed: Int = 0,
    val sessionStartedAt: Long? = null,
    val activeMs: Long = 0L,
    val lastSeenAt: Long? = null,
    val frozenUntil: Long = 0L
) {
    val isSessionRunning: Boolean get() = sessionStartedAt != null
}

/** Runtime ulaz za engine — sve što nije korisnička konfiguracija. */
data class RuntimeState(
    val todayUsageMs: Map<String, Long> = emptyMap(),
    val wakeUpBlockedUntil: Long = 0L,
    val shortsPenaltyUntil: Map<String, Long> = emptyMap(),
    val sessionStates: Map<String, SessionState> = emptyMap()
)

enum class BlockReason(val message: String) {
    SCHEDULE("Zakazana blokada je aktivna"),
    DAILY_LIMIT("Dnevni limit je potrošen"),
    SESSION_COOLDOWN("Pauza između sesija"),
    SESSION_POOL_EXHAUSTED("Iskorišćene su sve sesije za danas"),
    SHORTS_PENALTY("Shorts/Reels limit je potrošen"),
    WAKE_UP("Jutarnja blokada je aktivna")
}

data class BlockDecision(
    val packageName: String,
    val reason: BlockReason,
    val blockedUntil: Long,
    val ruleName: String? = null
) {
    val message: String
        get() = if (ruleName.isNullOrBlank()) reason.message else "${reason.message} · $ruleName"

    fun remainingMs(now: Long = System.currentTimeMillis()): Long = (blockedUntil - now).coerceAtLeast(0)
}
