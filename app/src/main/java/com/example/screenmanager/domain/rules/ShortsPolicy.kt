package com.example.screenmanager.domain.rules

import com.example.screenmanager.model.SessionLimitRule
import com.example.screenmanager.model.ShortVideoConfig
import com.example.screenmanager.model.ShortsMode
import java.time.ZoneId

/** Šta accessibility servis treba da uradi kad je Shorts/Reels plejer vidljiv. */
sealed interface ShortsVerdict {
    /** Gledanje je dozvoljeno (sesija traje / budžet nije potrošen). */
    data object Allow : ShortsVerdict

    /**
     * Izbaci korisnika iz Shorts/Reels (GLOBAL_ACTION_BACK).
     * @property until do kada važi zabrana (null = trajno dok je mod BLOCKED).
     */
    data class Kick(val reason: String, val until: Long?) : ShortsVerdict
}

/**
 * Čista logika za modove [ShortsMode.BLOCKED] i [ShortsMode.SESSIONS].
 * [ShortsMode.BUDGET] ostaje u servisu jer zavisi od akumulacije vremena gledanja.
 *
 * Interval mod ponovo koristi [SessionLimitTracker] sa sintetičkim pravilom
 * `shorts:<paket>` — tick se dešava samo dok je plejer vidljiv, a tracker sam
 * zatvara sesiju ako je rupa između dva viđenja duža od grace perioda (30 s).
 */
object ShortsPolicy {
    const val STATE_PREFIX = "shorts:"

    fun stateKey(packageName: String) = STATE_PREFIX + packageName

    fun sessionRule(config: ShortVideoConfig, packageName: String) = SessionLimitRule(
        id = stateKey(packageName),
        name = "Shorts/Reels",
        selectedAppIds = listOf(packageName),
        sessionLengthMinutes = config.sessionLengthMinutes,
        maxSessions = config.maxSessions,
        cooldownMinutes = config.cooldownMinutes
    )

    /**
     * Poziva se na svako viđenje Shorts/Reels plejera.
     * @return novo stanje sesije (samo za SESSIONS) i odluka.
     */
    fun onVisible(
        config: ShortVideoConfig,
        packageName: String,
        previous: SessionState?,
        now: Long,
        zone: ZoneId,
        startOfNextDay: Long
    ): Pair<SessionState?, ShortsVerdict> = when (config.mode) {
        ShortsMode.BLOCKED -> previous to ShortsVerdict.Kick("Shorts/Reels are blocked", null)
        ShortsMode.BUDGET -> previous to ShortsVerdict.Allow
        ShortsMode.SESSIONS -> {
            val next = SessionLimitTracker.advance(sessionRule(config, packageName), previous, true, now, zone)
            val verdict = when {
                next.isSessionRunning -> ShortsVerdict.Allow
                next.sessionsUsed >= config.maxSessions ->
                    ShortsVerdict.Kick("All Shorts/Reels sessions for today are used up", startOfNextDay)
                else -> ShortsVerdict.Kick("Shorts/Reels break", next.frozenUntil)
            }
            next to verdict
        }
    }
}
