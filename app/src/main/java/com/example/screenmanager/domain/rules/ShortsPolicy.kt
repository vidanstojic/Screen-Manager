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
 * Svi short-form izvori (YouTube Shorts, Instagram Reels) dele JEDNO stanje:
 * jedna sesija M/N/K i jedan dnevni budžet. Ranije je svaka aplikacija imala
 * svoj brojač, pa je posle blokade Reels-a prelazak na Shorts otvarao novu,
 * netaknutu sesiju (BUG-002).
 *
 * Interval mod koristi [SessionLimitTracker] sa sintetičkim pravilom
 * [SHARED_STATE_KEY]; tick se dešava samo dok je plejer vidljiv, a tracker sam
 * zatvara sesiju ako je rupa između dva viđenja duža od grace perioda (30 s).
 */
object ShortsPolicy {
    const val STATE_PREFIX = "shorts:"

    /** Ključ jedinog interval stanja za sve short-form aplikacije. */
    const val SHARED_STATE_KEY = STATE_PREFIX + "all"

    /** Ključ zajedničkog dnevnog budžeta gledanja (BUDGET mod). */
    const val SHARED_WATCH_KEY = "all"

    fun sessionRule(config: ShortVideoConfig) = SessionLimitRule(
        id = SHARED_STATE_KEY,
        name = "Shorts/Reels",
        selectedAppIds = config.selectedAppIds,
        sessionLengthMinutes = config.sessionLengthMinutes,
        maxSessions = config.maxSessions,
        cooldownMinutes = config.cooldownMinutes
    )

    /**
     * Poziva se na svako viđenje Shorts/Reels plejera u bilo kojoj od izabranih aplikacija.
     * @return novo stanje sesije (samo za SESSIONS) i odluka.
     */
    fun onVisible(
        config: ShortVideoConfig,
        previous: SessionState?,
        now: Long,
        zone: ZoneId,
        startOfNextDay: Long
    ): Pair<SessionState?, ShortsVerdict> = when (config.mode) {
        ShortsMode.BLOCKED -> previous to ShortsVerdict.Kick("Shorts/Reels are blocked", null)
        ShortsMode.BUDGET -> previous to ShortsVerdict.Allow
        ShortsMode.SESSIONS -> {
            val next = SessionLimitTracker.advance(sessionRule(config), previous, true, now, zone)
            val verdict = when {
                next.isSessionRunning -> ShortsVerdict.Allow
                next.sessionsUsed >= config.maxSessions ->
                    ShortsVerdict.Kick("All Shorts/Reels sessions for today are used up", startOfNextDay)
                else -> ShortsVerdict.Kick("Shorts/Reels break", next.frozenUntil)
            }
            next to verdict
        }
    }

    /**
     * Dnevni budžet (BUDGET) je zajednički: [watchedTodayMs] je zbir gledanja u
     * svim izabranim aplikacijama. Kad se potroši, izbacuje se iz Shorts/Reels
     * svake od njih do ponoći.
     */
    fun budgetExhausted(config: ShortVideoConfig, watchedTodayMs: Long): Boolean =
        watchedTodayMs >= config.maxReelsWatchMinutes.coerceAtLeast(0) * 60_000L
}
