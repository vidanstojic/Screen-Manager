package com.example.screenmanager.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.screenmanager.domain.rules.LimitLocks
import com.example.screenmanager.domain.rules.LimitsLock
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Čuva [LimitLocks] (SharedPreferences, kao [UiPreferences]): opšte zaključavanje u tri
 * ključa, pojedinačna kao skup redova `ključPravila\tzid\telapsed\ttrajanje`.
 * Briše se samo brisanjem podataka aplikacije.
 */
class LimitsLockStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun current(): LimitLocks = LimitLocks(all = readAll(), rules = readRules())

    fun observe(): Flow<LimitLocks> = callbackFlow {
        trySend(current())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(current()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun save(locks: LimitLocks) {
        val editor = prefs.edit()
        val all = locks.all
        if (all == null) {
            editor.remove(KEY_LOCKED_AT_WALL).remove(KEY_LOCKED_AT_ELAPSED).remove(KEY_DURATION)
        } else {
            editor.putLong(KEY_LOCKED_AT_WALL, all.lockedAtWall)
                .putLong(KEY_LOCKED_AT_ELAPSED, all.lockedAtElapsed)
                .putLong(KEY_DURATION, all.durationMs)
        }
        editor.putStringSet(
            KEY_RULE_LOCKS,
            locks.rules.map { (key, lock) -> listOf(key, lock.lockedAtWall, lock.lockedAtElapsed, lock.durationMs).joinToString(SEPARATOR) }.toSet()
        )
        editor.apply()
    }

    private fun readAll(): LimitsLock? {
        val duration = prefs.getLong(KEY_DURATION, 0L)
        if (duration <= 0L) return null
        return LimitsLock(
            lockedAtWall = prefs.getLong(KEY_LOCKED_AT_WALL, 0L),
            lockedAtElapsed = prefs.getLong(KEY_LOCKED_AT_ELAPSED, 0L),
            durationMs = duration
        )
    }

    private fun readRules(): Map<String, LimitsLock> =
        prefs.getStringSet(KEY_RULE_LOCKS, emptySet()).orEmpty().mapNotNull { line ->
            val parts = line.split(SEPARATOR)
            if (parts.size != 4) return@mapNotNull null
            val wall = parts[1].toLongOrNull() ?: return@mapNotNull null
            val elapsed = parts[2].toLongOrNull() ?: return@mapNotNull null
            val duration = parts[3].toLongOrNull() ?: return@mapNotNull null
            parts[0] to LimitsLock(wall, elapsed, duration)
        }.toMap()

    private companion object {
        const val FILE_NAME = "limits_lock"
        const val KEY_LOCKED_AT_WALL = "locked_at_wall"
        const val KEY_LOCKED_AT_ELAPSED = "locked_at_elapsed"
        const val KEY_DURATION = "duration_ms"
        const val KEY_RULE_LOCKS = "rule_locks"
        const val SEPARATOR = "\t"
    }
}
