package com.example.screenmanager.alarm.domain

import com.example.screenmanager.model.WakeCheckConfig

/**
 * Zakazana provera "Da li si budan?".
 *
 * @property round redni broj provere za ovo buđenje (1 = prva posle gašenja alarma).
 * @property askAt kada se pitanje postavlja.
 * @property deadline do kada korisnik mora da potvrdi; null dok pitanje još nije postavljeno.
 */
data class PendingWakeCheck(
    val alarmId: Long,
    val round: Int,
    val askAt: Long,
    val deadline: Long? = null
)

/**
 * Wake-up check bez Android zavisnosti:
 * gašenje alarma → posle `delayMinutes` pitanje → bez potvrde u roku od
 * [WakeCheckConfig.RESPONSE_WINDOW_SECONDS] alarm ponovo zvoni → posle
 * njegovog gašenja nova provera, najviše [MAX_ROUNDS] puta.
 */
object WakeCheckPolicy {
    const val MAX_ROUNDS = 3
    const val RESPONSE_WINDOW_MS = WakeCheckConfig.RESPONSE_WINDOW_SECONDS * 1_000L

    /**
     * Provera koja se zakazuje kad se alarm ugasi, ili null.
     * [currentRound] je runda koja je izazvala ovo zvonjenje (0 ako ga nije
     * izazvala provera).
     */
    fun afterDismiss(
        config: WakeCheckConfig,
        alarmId: Long,
        reason: RingReason,
        currentRound: Int,
        now: Long
    ): PendingWakeCheck? {
        if (!config.enabled || reason == RingReason.TEST) return null
        val nextRound = if (reason == RingReason.WAKE_CHECK) currentRound + 1 else 1
        if (nextRound > MAX_ROUNDS) return null
        val delayMinutes = config.delayMinutes.coerceIn(1, 60)
        return PendingWakeCheck(alarmId, nextRound, askAt = now + delayMinutes * 60_000L)
    }

    /** Pitanje je postavljeno u [now]: od sada teče rok za potvrdu. */
    fun asked(check: PendingWakeCheck, now: Long): PendingWakeCheck = check.copy(deadline = now + RESPONSE_WINDOW_MS)

    /** Da li istek roka treba da ponovo pokrene alarm (provera nije potvrđena niti zamenjena novom). */
    fun shouldRingAgain(pending: PendingWakeCheck?, alarmId: Long, round: Int): Boolean =
        pending != null && pending.alarmId == alarmId && pending.round == round && pending.deadline != null
}
