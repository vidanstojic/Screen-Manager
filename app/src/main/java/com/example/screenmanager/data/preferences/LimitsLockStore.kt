package com.example.screenmanager.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.screenmanager.domain.rules.LimitsLock
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Čuva [LimitsLock] (SharedPreferences, kao [UiPreferences]); briše se samo brisanjem podataka aplikacije. */
class LimitsLockStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun current(): LimitsLock? {
        val duration = prefs.getLong(KEY_DURATION, 0L)
        if (duration <= 0L) return null
        return LimitsLock(
            lockedAtWall = prefs.getLong(KEY_LOCKED_AT_WALL, 0L),
            lockedAtElapsed = prefs.getLong(KEY_LOCKED_AT_ELAPSED, 0L),
            durationMs = duration
        )
    }

    fun observe(): Flow<LimitsLock?> = callbackFlow {
        trySend(current())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(current()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun save(lock: LimitsLock) {
        prefs.edit()
            .putLong(KEY_LOCKED_AT_WALL, lock.lockedAtWall)
            .putLong(KEY_LOCKED_AT_ELAPSED, lock.lockedAtElapsed)
            .putLong(KEY_DURATION, lock.durationMs)
            .apply()
    }

    private companion object {
        const val FILE_NAME = "limits_lock"
        const val KEY_LOCKED_AT_WALL = "locked_at_wall"
        const val KEY_LOCKED_AT_ELAPSED = "locked_at_elapsed"
        const val KEY_DURATION = "duration_ms"
    }
}
