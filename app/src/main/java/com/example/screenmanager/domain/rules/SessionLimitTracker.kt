package com.example.screenmanager.domain.rules

import com.example.screenmanager.model.SessionLimitRule
import java.time.Instant
import java.time.ZoneId

/**
 * Čista state-mašina za interval mod (M/N/K).
 *
 * Poziva se na svaki tick monitoring servisa sa informacijom da li je neka
 * aplikacija iz grupe pravila trenutno u foreground-u.
 */
object SessionLimitTracker {
    /** Povratak u aplikaciju u ovom roku nastavlja istu sesiju. */
    const val RESUME_GRACE_MS: Long = 30_000L

    /** Maksimalni doprinos jednog tick-a (štiti od skokova kad servis "zaspi"). */
    const val MAX_TICK_GAP_MS: Long = 10_000L

    fun advance(
        rule: SessionLimitRule,
        previous: SessionState?,
        foregroundInRule: Boolean,
        now: Long,
        zone: ZoneId
    ): SessionState {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().toEpochDay()
        var state = previous?.takeIf { it.epochDay == today } ?: SessionState(rule.id, today)

        val sessionLengthMs = rule.sessionLengthMinutes.coerceAtLeast(1) * 60_000L
        val cooldownMs = rule.cooldownMinutes.coerceAtLeast(0) * 60_000L

        if (state.isSessionRunning) {
            val lastSeen = state.lastSeenAt ?: now
            if (now - lastSeen > RESUME_GRACE_MS) {
                // Rupa duža od grace perioda (korisnik je otišao, ekran je bio
                // ugašen ili servis nije radio) → sesija je završena u lastSeen,
                // pa se dalje ponašamo kao da sesija ne traje.
                state = state.endSession(frozenUntil = lastSeen + cooldownMs)
            } else {
                if (foregroundInRule) {
                    val delta = (now - lastSeen).coerceIn(0L, MAX_TICK_GAP_MS)
                    val active = state.activeMs + delta
                    state = if (active >= sessionLengthMs) {
                        state.endSession(frozenUntil = now + cooldownMs)
                    } else {
                        state.copy(activeMs = active, lastSeenAt = now)
                    }
                }
                return state
            }
        }

        if (!foregroundInRule) return state
        if (state.frozenUntil > now) return state
        if (state.sessionsUsed >= rule.maxSessions) return state

        return state.startSession(now)
    }

    private fun SessionState.startSession(now: Long) = copy(
        sessionsUsed = sessionsUsed + 1,
        sessionStartedAt = now,
        activeMs = 0L,
        lastSeenAt = now
    )

    private fun SessionState.endSession(frozenUntil: Long) = copy(
        sessionStartedAt = null,
        activeMs = 0L,
        frozenUntil = frozenUntil
    )
}
