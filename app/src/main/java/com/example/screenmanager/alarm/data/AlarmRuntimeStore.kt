package com.example.screenmanager.alarm.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Odloženo zvonjenje: alarm [alarmId] ponovo zvoni u [at]; [count] = koliko je puta već odložen. */
data class PendingSnooze(val alarmId: Long, val at: Long, val count: Int)

/**
 * Runtime stanje alarma koje mora da preživi gašenje procesa i restart
 * telefona (odlaganja). Čuva se u SharedPreferences — mali broj ključeva,
 * nije deo korisničkih pravila u Room bazi.
 *
 * Svi pozivi su iz istog procesa, pa [snoozes] uvek odgovara stanju na disku.
 */
class AlarmRuntimeStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _snoozes = MutableStateFlow(readSnoozes())

    /** Sva aktivna odlaganja, po alarmu. */
    val snoozes: StateFlow<Map<Long, PendingSnooze>> = _snoozes.asStateFlow()

    fun putSnooze(snooze: PendingSnooze) {
        prefs.edit()
            .putLong(SNOOZE_AT + snooze.alarmId, snooze.at)
            .putInt(SNOOZE_COUNT + snooze.alarmId, snooze.count)
            .apply()
        _snoozes.value = readSnoozes()
    }

    fun removeSnooze(alarmId: Long) {
        prefs.edit().remove(SNOOZE_AT + alarmId).remove(SNOOZE_COUNT + alarmId).apply()
        _snoozes.value = readSnoozes()
    }

    private fun readSnoozes(): Map<Long, PendingSnooze> =
        prefs.all.keys
            .filter { it.startsWith(SNOOZE_AT) }
            .mapNotNull { key -> key.removePrefix(SNOOZE_AT).toLongOrNull() }
            .associateWith { id ->
                PendingSnooze(id, prefs.getLong(SNOOZE_AT + id, 0L), prefs.getInt(SNOOZE_COUNT + id, 0))
            }

    companion object {
        private const val PREFS_NAME = "alarm_runtime"
        private const val SNOOZE_AT = "snooze_at:"
        private const val SNOOZE_COUNT = "snooze_count:"
    }
}
