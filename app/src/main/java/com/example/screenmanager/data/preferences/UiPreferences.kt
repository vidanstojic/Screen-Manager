package com.example.screenmanager.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Korisnička podešavanja prikaza (ne pravila blokiranja — ona su u Room bazi).
 *
 * Čuva se u SharedPreferences: mali broj vrednosti, bez migracija baze.
 * Za sada: dnevni cilj vremena ekrana (prsten na Overview ekranu).
 */
class UiPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    /** Dnevni cilj u minutima, ili `null` ako cilj nije postavljen. */
    fun observeDailyGoalMinutes(): Flow<Int?> = callbackFlow {
        trySend(dailyGoalMinutes())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_DAILY_GOAL_MINUTES) trySend(dailyGoalMinutes())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun dailyGoalMinutes(): Int? =
        prefs.getInt(KEY_DAILY_GOAL_MINUTES, NO_GOAL).takeIf { it > 0 }

    /** `null` (ili 0) uklanja cilj. */
    fun setDailyGoalMinutes(minutes: Int?) {
        prefs.edit().putInt(KEY_DAILY_GOAL_MINUTES, minutes?.coerceAtLeast(0) ?: NO_GOAL).apply()
    }

    companion object {
        private const val FILE_NAME = "ui_preferences"
        private const val KEY_DAILY_GOAL_MINUTES = "daily_goal_minutes"
        private const val NO_GOAL = 0

        /** Vrednost koju cilj dobija kad ga korisnik prvi put uključi. */
        const val DEFAULT_GOAL_MINUTES = 4 * 60
    }
}
