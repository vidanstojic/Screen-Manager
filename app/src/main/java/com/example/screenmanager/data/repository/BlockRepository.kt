package com.example.screenmanager.data.repository

import com.example.screenmanager.data.local.AppInternalState
import com.example.screenmanager.data.local.AppInternalStateDao
import com.example.screenmanager.data.local.BlockConfig
import com.example.screenmanager.data.local.BlockConfigDao
import com.example.screenmanager.domain.AppStateKeys
import com.example.screenmanager.domain.BlockDecision
import kotlinx.coroutines.flow.Flow

class BlockRepository(
    private val blockConfigDao: BlockConfigDao,
    private val stateDao: AppInternalStateDao
) {
    fun observeConfigs(): Flow<List<BlockConfig>> = blockConfigDao.observeAll()
    fun observeWakeupBlockedUntil(): Flow<Long?> = stateDao.observeLong(AppStateKeys.WAKEUP_BLOCKED_UNTIL)
    fun observeShortsBlockedUntil(): Flow<Long?> = stateDao.observeLong(AppStateKeys.SHORTS_BLOCKED_UNTIL)

    suspend fun seedDefaultsIfNeeded() {
        if (blockConfigDao.wakeupBlockedPackages().isNotEmpty()) return
        blockConfigDao.upsertAll(
            listOf(
                BlockConfig(
                    "com.instagram.android",
                    isBlockedDuringWakeup = true,
                    maxDailyAllowedMinutes = 30
                ),
                BlockConfig(
                    "com.google.android.youtube",
                    isBlockedDuringWakeup = true,
                    maxDailyAllowedMinutes = 45
                ),
                BlockConfig(
                    "com.zhiliaoapp.musically",
                    isBlockedDuringWakeup = true,
                    maxDailyAllowedMinutes = 20
                ),
                BlockConfig(
                    "com.twitter.android",
                    isBlockedDuringWakeup = true,
                    maxDailyAllowedMinutes = 20
                )
            )
        )
    }

    suspend fun upsertConfig(config: BlockConfig) = blockConfigDao.upsert(config)

    suspend fun getBlockDecision(packageName: String, now: Long = System.currentTimeMillis()): BlockDecision? {
        val config = blockConfigDao.find(packageName)
        val wakeupUntil = stateDao.getLong(AppStateKeys.WAKEUP_BLOCKED_UNTIL) ?: 0L
        if (config?.isBlockedDuringWakeup == true && wakeupUntil > now) {
            return BlockDecision(packageName, "Jutarnja blokada je aktivna", wakeupUntil)
        }

        val shortsUntil = stateDao.getLong(AppStateKeys.SHORTS_BLOCKED_UNTIL) ?: 0L
        if (packageName in shortsPackages && shortsUntil > now) {
            return BlockDecision(packageName, "Shorts/Reels kazna je aktivna", shortsUntil)
        }

        return null
    }

    suspend fun activateWakeupBlock(blockedUntil: Long) {
        stateDao.upsert(AppInternalState(AppStateKeys.WAKEUP_BLOCKED_UNTIL, blockedUntil))
    }

    suspend fun markScreenOff(timestamp: Long) {
        stateDao.upsert(AppInternalState(AppStateKeys.LAST_SCREEN_OFF_AT, timestamp))
        stateDao.delete(AppStateKeys.POTENTIAL_WAKEUP_AT)
    }

    suspend fun markScreenOn(timestamp: Long) {
        stateDao.upsert(AppInternalState(AppStateKeys.LAST_SCREEN_ON_AT, timestamp))
    }

    suspend fun getLastScreenOff(): Long? = stateDao.getLong(AppStateKeys.LAST_SCREEN_OFF_AT)

    suspend fun markPotentialWakeup(timestamp: Long) {
        stateDao.upsert(AppInternalState(AppStateKeys.POTENTIAL_WAKEUP_AT, timestamp))
    }

    suspend fun clearPotentialWakeup() {
        stateDao.delete(AppStateKeys.POTENTIAL_WAKEUP_AT)
    }

    suspend fun markInteraction(timestamp: Long) {
        stateDao.upsert(AppInternalState(AppStateKeys.LAST_USER_INTERACTION_AT, timestamp))
    }

    suspend fun getLastInteraction(): Long? = stateDao.getLong(AppStateKeys.LAST_USER_INTERACTION_AT)

    suspend fun punishShorts(blockedUntil: Long) {
        stateDao.upsert(AppInternalState(AppStateKeys.SHORTS_BLOCKED_UNTIL, blockedUntil))
    }

    suspend fun getShortsBlockedUntil(): Long = stateDao.getLong(AppStateKeys.SHORTS_BLOCKED_UNTIL) ?: 0L

    companion object {
        val shortsPackages = setOf("com.google.android.youtube", "com.instagram.android")
    }
}