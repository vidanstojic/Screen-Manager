package com.example.screenmanager.data.repository

import com.example.screenmanager.data.local.AppInternalState
import com.example.screenmanager.data.local.AppInternalStateDao
import com.example.screenmanager.data.local.SessionLimitDao
import com.example.screenmanager.data.local.toDomain
import com.example.screenmanager.data.local.toEntity
import com.example.screenmanager.domain.AppStateKeys
import com.example.screenmanager.domain.rules.SessionState
import com.example.screenmanager.domain.wakeup.WakeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Runtime stanje blokiranja (ne korisnička pravila!).
 *
 * Zamenjuje stari BlockRepository koji je mešao hardkodovana pravila
 * (BlockConfig seed) sa stanjem. Pravila su sada isključivo u
 * [SettingsRepository], a ovde su samo "činjenice": do kada traje koja
 * kazna, koliko je Shorts-a odgledano, stanje interval sesija...
 */
class RuntimeStateRepository(
    private val stateDao: AppInternalStateDao,
    private val sessionLimitDao: SessionLimitDao
) {
    // --- Wake-up lockout ---

    fun observeWakeUpBlockedUntil(): Flow<Long?> = stateDao.observeLong(AppStateKeys.WAKEUP_BLOCKED_UNTIL)

    suspend fun wakeUpBlockedUntil(): Long = stateDao.getLong(AppStateKeys.WAKEUP_BLOCKED_UNTIL) ?: 0L

    suspend fun setWakeUpBlockedUntil(until: Long) = put(AppStateKeys.WAKEUP_BLOCKED_UNTIL, until)

    // --- Detekcija buđenja ---

    suspend fun wakeState(): WakeState = WakeState(
        // Prvi put posle prelaska sa stare heuristike noć počinje od poslednjeg gašenja ekrana.
        darkSince = stateDao.getLong(AppStateKeys.WAKE_DARK_SINCE) ?: stateDao.getLong(AppStateKeys.LAST_SCREEN_OFF_AT),
        screenOnAt = stateDao.getLong(AppStateKeys.WAKE_SCREEN_ON_AT),
        wakeAt = stateDao.getLong(AppStateKeys.WAKE_CANDIDATE_AT),
        wakeOnMs = stateDao.getLong(AppStateKeys.WAKE_CANDIDATE_ON_MS) ?: 0L
    )

    suspend fun saveWakeState(state: WakeState) {
        putOrDelete(AppStateKeys.WAKE_DARK_SINCE, state.darkSince)
        putOrDelete(AppStateKeys.WAKE_SCREEN_ON_AT, state.screenOnAt)
        putOrDelete(AppStateKeys.WAKE_CANDIDATE_AT, state.wakeAt)
        put(AppStateKeys.WAKE_CANDIDATE_ON_MS, state.wakeOnMs)
    }

    // --- Shorts / Reels ---

    fun observeShortsPenalties(): Flow<Map<String, Long>> =
        stateDao.observeWithPrefix(AppStateKeys.SHORTS_PENALTY_PREFIX)
            .map { it.toPackageMap(AppStateKeys.SHORTS_PENALTY_PREFIX) }

    suspend fun setShortsPenalty(packageName: String, until: Long) =
        put(AppStateKeys.SHORTS_PENALTY_PREFIX + packageName, until)

    /** Danas odgledano short-form vreme pod ključem [key] (resetuje se promenom dana). */
    suspend fun shortsWatchedMs(key: String, epochDay: Long): Long {
        val day = stateDao.getLong(AppStateKeys.SHORTS_WATCHED_DAY_PREFIX + key)
        if (day != epochDay) return 0L
        return stateDao.getLong(AppStateKeys.SHORTS_WATCHED_PREFIX + key) ?: 0L
    }

    /** Dodaje [deltaMs] na današnji zbir pod ključem [key] i vraća novi zbir. */
    suspend fun addShortsWatchedMs(key: String, epochDay: Long, deltaMs: Long): Long {
        val total = shortsWatchedMs(key, epochDay) + deltaMs
        put(AppStateKeys.SHORTS_WATCHED_DAY_PREFIX + key, epochDay)
        put(AppStateKeys.SHORTS_WATCHED_PREFIX + key, total)
        return total
    }

    // --- Interval mod ---

    suspend fun sessionStates(): Map<String, SessionState> =
        sessionLimitDao.allStates().associate { it.ruleId to it.toDomain() }

    suspend fun sessionState(ruleId: String): SessionState? = sessionLimitDao.state(ruleId)?.toDomain()

    suspend fun saveSessionStates(states: Collection<SessionState>) {
        if (states.isNotEmpty()) sessionLimitDao.upsertStates(states.map { it.toEntity() })
    }

    private suspend fun put(key: String, value: Long) = stateDao.upsert(AppInternalState(key, value))

    private suspend fun putOrDelete(key: String, value: Long?) {
        if (value == null) stateDao.delete(key) else put(key, value)
    }

    private fun List<AppInternalState>.toPackageMap(prefix: String): Map<String, Long> =
        associate { it.stateKey.removePrefix(prefix) to it.stateValueLong }
}
