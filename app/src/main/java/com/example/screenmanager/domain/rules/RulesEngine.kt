package com.example.screenmanager.domain.rules

import com.example.screenmanager.model.SessionLimitRule
import com.example.screenmanager.model.ShortsMode
import java.time.Instant
import java.time.ZoneId

/**
 * Jedini izvor istine za odluku "da li blokirati paket X u trenutku T".
 *
 * Čista funkcija bez Android zavisnosti: sve ulaze dobija eksplicitno
 * ([RulesSnapshot] + [RuntimeState] + `now` + [ZoneId]). Zamenjuje stari
 * BlockingEngine (mrtav kod sa stub-ovima) i hardkodovanu logiku iz
 * BlockRepository/TimeBuckets.
 *
 * Kada više pravila važi istovremeno, vraća se ono sa NAJKASNIJIM krajem
 * blokade — korisnik vidi realno vreme do otključavanja.
 */
class RulesEngine {

    fun evaluate(
        packageName: String,
        now: Long,
        zone: ZoneId,
        rules: RulesSnapshot,
        state: RuntimeState
    ): BlockDecision? {
        if (rules.emergency?.isActiveAt(now) == true) return null

        val candidates = ArrayList<BlockDecision>(4)

        rules.schedules
            .filter { it.isEnabled && packageName in it.selectedAppIds }
            .forEach { rule ->
                ScheduleWindows.activeUntil(rule, now, zone)?.let {
                    candidates += BlockDecision(packageName, BlockReason.SCHEDULE, it, rule.name)
                }
            }

        val midnight = startOfNextDay(now, zone)

        rules.appLimits
            .filter { it.isEnabled && packageName in it.selectedAppIds }
            .forEach { rule ->
                val groupUsage = rule.selectedAppIds.distinct().sumOf { state.todayUsageMs[it] ?: 0L }
                if (groupUsage >= rule.dailyLimitMinutes * 60_000L) {
                    candidates += BlockDecision(packageName, BlockReason.DAILY_LIMIT, midnight, rule.name)
                }
            }

        rules.sessionLimits
            .filter { it.isEnabled && packageName in it.selectedAppIds }
            .forEach { rule -> sessionDecision(packageName, rule, state, now, zone, midnight)?.let(candidates::add) }

        rules.shortVideo?.let { config ->
            val penaltyUntil = state.shortsPenaltyUntil[packageName] ?: 0L
            // Kazna za celu aplikaciju postoji samo u BUDGET modu.
            if (config.isEnabled && config.mode == ShortsMode.BUDGET &&
                packageName in config.selectedAppIds && penaltyUntil > now
            ) {
                candidates += BlockDecision(packageName, BlockReason.SHORTS_PENALTY, penaltyUntil)
            }
        }

        rules.wakeUp?.let { config ->
            if (config.isEnabled && packageName in config.selectedAppIds && state.wakeUpBlockedUntil > now) {
                candidates += BlockDecision(packageName, BlockReason.WAKE_UP, state.wakeUpBlockedUntil)
            }
        }

        return candidates.maxByOrNull { it.blockedUntil }
    }

    private fun sessionDecision(
        packageName: String,
        rule: SessionLimitRule,
        state: RuntimeState,
        now: Long,
        zone: ZoneId,
        midnight: Long
    ): BlockDecision? {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().toEpochDay()
        val session = state.sessionStates[rule.id]?.takeIf { it.epochDay == today } ?: return null
        if (session.isSessionRunning) return null
        return when {
            session.sessionsUsed >= rule.maxSessions ->
                BlockDecision(packageName, BlockReason.SESSION_POOL_EXHAUSTED, midnight, rule.name)
            session.frozenUntil > now ->
                BlockDecision(packageName, BlockReason.SESSION_COOLDOWN, session.frozenUntil, rule.name)
            else -> null
        }
    }

    private fun startOfNextDay(now: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(now).atZone(zone).toLocalDate().plusDays(1)
            .atStartOfDay(zone).toInstant().toEpochMilli()
}
