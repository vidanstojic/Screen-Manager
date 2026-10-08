package com.example.screenmanager.alarm.data

import android.content.Context
import com.example.screenmanager.alarm.domain.AlarmDetoxLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Aktivna wake-up detox blokada (preživljava restart procesa i telefona —
 * kraj je apsolutni trenutak). Čitaju je FocusMonitorService (aplikacije)
 * i WebsiteGuard (sajtovi) preko [lock].
 */
class AlarmDetoxStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _lock = MutableStateFlow(read())

    /** Poslednja zapisana blokada; može biti istekla — proveriti [AlarmDetoxLock.isActiveAt]. */
    val lock: StateFlow<AlarmDetoxLock?> = _lock.asStateFlow()

    fun start(lock: AlarmDetoxLock) {
        prefs.edit()
            .putLong(KEY_UNTIL, lock.until)
            .putStringSet(KEY_PACKAGES, lock.packages)
            .putStringSet(KEY_DOMAINS, lock.domains)
            .putString(KEY_LABEL, lock.label)
            .apply()
        _lock.value = lock
    }

    fun end() {
        prefs.edit().clear().apply()
        _lock.value = null
    }

    private fun read(): AlarmDetoxLock? {
        val until = prefs.getLong(KEY_UNTIL, 0L)
        if (until == 0L) return null
        return AlarmDetoxLock(
            until = until,
            packages = prefs.getStringSet(KEY_PACKAGES, emptySet()).orEmpty().toSet(),
            domains = prefs.getStringSet(KEY_DOMAINS, emptySet()).orEmpty().toSet(),
            label = prefs.getString(KEY_LABEL, null).orEmpty()
        )
    }

    private companion object {
        const val PREFS_NAME = "alarm_detox"
        const val KEY_UNTIL = "until"
        const val KEY_PACKAGES = "packages"
        const val KEY_DOMAINS = "domains"
        const val KEY_LABEL = "label"
    }
}
