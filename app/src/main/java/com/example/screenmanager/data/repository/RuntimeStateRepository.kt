package com.example.screenmanager.data.repository

import com.example.screenmanager.data.local.AppInternalState
import com.example.screenmanager.data.local.AppInternalStateDao
import com.example.screenmanager.data.local.SessionLimitDao
import com.example.screenmanager.data.local.toDomain
import com.example.screenmanager.data.local.toEntity
import com.example.screenmanager.domain.AppStateKeys
import com.example.screenmanager.domain.rules.SessionState
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

    // --- Ekran / aktivnost ---

    suspend fun markScreenOff(timestamp: Long) = put(AppStateKeys.LAST_SCREEN_OFF_AT, timestamp)

    suspend fun markScreenOn(timestamp: Long) = put(AppStateKeys.LAST_SCREEN_ON_AT, timestamp)

    suspend fun lastScreenOff(): Long? = stateDao.getLong(AppStateKeys.LAST_SCREEN_OFF_AT)

    suspend fun markInteraction(timestamp: Long) = put(AppStateKeys.LAST_USER_INTERACTION_AT, timestamp)

    suspend fun lastInteraction(): Long? = stateDao.getLong(AppStateKeys.LAST_USER_INTERACTION_AT)

    // --- Shorts / Reels ---

    suspend fun shortsPenalties(): Map<String, Long> =
        stateDao.getWithPrefix(AppStateKeys.SHORTS_PENALTY_PREFIX).toPackageMap(AppStateKeys.SHORTS_PENALTY_PREFIX)

    fun observeShortsPenalties(): Flow<Map<String, Long>> =
        stateDao.observeWithPrefix(AppStateKeys.SHORTS_PENALTY_PREFIX)
            .map { it.toPackageMap(AppStateKeys.SHORTS_PENALTY_PREFIX) }

    suspend fun setShortsPenalty(packageName: String, until: Long) =
        put(AppStateKeys.SHORTS_PENALTY_PREFIX + packageName, until)

    /** Danas odgledano short-form vreme za paket (resetuje se promenom dana). */
    suspend fun shortsWatchedMs(packageName: String, epochDay: Long): Long {
        val day = stateDao.getLong(AppStateKeys.SHORTS_WATCHED_DAY_PREFIX + packageName)
        if (day != epochDay) return 0L
        return stateDao.getLong(AppStateKeys.SHORTS_WATCHED_PREFIX + packageName) ?: 0L
    }

    /** Dodaje [deltaMs] na današnji zbir i vraća novi zbir. */
    suspend fun addShortsWatchedMs(packageName: String, epochDay: Long, deltaMs: Long): Long {
        val total = shortsWatchedMs(packageName, epochDay) + deltaMs
        put(AppStateKeys.SHORTS_WATCHED_DAY_PREFIX + packageName, epochDay)
        put(AppStateKeys.SHORTS_WATCHED_PREFIX + packageName, total)
        return total
    }

    // --- Interval mod ---

    suspend fun sessionStates(): Map<String, SessionState> =
        sessionLimitDao.allStates().associate { it.ruleId to it.toDomain() }

    fun observeSessionStates(): Flow<Map<String, SessionState>> =
        sessionLimitDao.observeStates().map { list -> list.associate { it.ruleId to it.toDomain() } }

    suspend fun sessionState(ruleId: String): SessionState? = sessionLimitDao.state(ruleId)?.toDomain()

    suspend fun saveSessionStates(states: Collection<SessionState>) {
        if (states.isNotEmpty()) sessionLimitDao.upsertStates(states.map { it.toEntity() })
    }

    private suspend fun put(key: String, value: Long) = stateDao.upsert(AppInternalState(key, value))

    private fun List<AppInternalState>.toPackageMap(prefix: String): Map<String, Long> =
        associate { it.stateKey.removePrefix(prefix) to it.stateValueLong }
}
