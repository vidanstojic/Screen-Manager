package com.example.screenmanager.alarm.data

import android.content.Context
import android.content.SharedPreferences
import android.os.UserManager
import com.example.screenmanager.model.AlarmRule
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.Base64

/**
 * Kopija zakazanih alarma u device-protected skladištu, dostupnom i PRE prvog
 * otključavanja posle restarta (direct boot). Room baza je u credential-encrypted
 * skladištu i tada nije dostupna, pa bez ove kopije alarm posle noćnog restarta
 * (npr. automatsko ažuriranje sistema) ne bi zazvonio.
 *
 * Kopija se piše pri svakom zakazivanju ([com.example.screenmanager.domain.AlarmScheduler]),
 * a Room ostaje jedini izvor istine čim se telefon otključa.
 */
class DirectBootAlarmStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.createDeviceProtectedStorageContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(alarm: AlarmRule) {
        val encoded = AlarmSnapshotCodec.encode(alarm) ?: return
        prefs.edit().putString(ALARM + alarm.id, encoded).apply()
    }

    fun remove(alarmId: Long) {
        prefs.edit().remove(ALARM + alarmId).apply()
    }

    fun find(alarmId: Long): AlarmRule? = prefs.getString(ALARM + alarmId, null)?.let(AlarmSnapshotCodec::decode)

    fun all(): List<AlarmRule> =
        prefs.all.filterKeys { it.startsWith(ALARM) }.values.mapNotNull { (it as? String)?.let(AlarmSnapshotCodec::decode) }

    /** Jednokratni alarm je zazvonio pre otključavanja: posle otključavanja treba ga isključiti i u bazi. */
    fun markFiredOnce(alarmId: Long) {
        prefs.edit().putBoolean(FIRED_ONCE + alarmId, true).remove(ALARM + alarmId).apply()
    }

    /** Vraća i briše oznake iz [markFiredOnce]. */
    fun consumeFiredOnce(): Set<Long> {
        val ids = prefs.all.keys.filter { it.startsWith(FIRED_ONCE) }.mapNotNull { it.removePrefix(FIRED_ONCE).toLongOrNull() }
        if (ids.isNotEmpty()) prefs.edit().apply { ids.forEach { remove(FIRED_ONCE + it) } }.apply()
        return ids.toSet()
    }

    companion object {
        private const val PREFS_NAME = "alarm_direct_boot"
        private const val ALARM = "alarm:"
        private const val FIRED_ONCE = "fired_once:"

        fun isUserUnlocked(context: Context): Boolean =
            context.getSystemService(UserManager::class.java)?.isUserUnlocked != false
    }
}

/** (De)serijalizacija [AlarmRule] u tekst za SharedPreferences. Neuspeh = null, nikad izuzetak. */
object AlarmSnapshotCodec {
    fun encode(alarm: AlarmRule): String? = runCatching {
        val bytes = ByteArrayOutputStream().also { out -> ObjectOutputStream(out).use { it.writeObject(alarm) } }.toByteArray()
        Base64.getEncoder().encodeToString(bytes)
    }.getOrNull()

    /** Snimak iz starije verzije aplikacije (izmenjen model) daje null; Room ga zameni posle otključavanja. */
    fun decode(text: String): AlarmRule? = runCatching {
        ObjectInputStream(ByteArrayInputStream(Base64.getDecoder().decode(text))).use { it.readObject() as AlarmRule }
    }.getOrNull()
}
