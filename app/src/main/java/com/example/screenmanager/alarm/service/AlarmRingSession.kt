package com.example.screenmanager.alarm.service

import com.example.screenmanager.alarm.domain.RingPolicy
import com.example.screenmanager.alarm.domain.RingReason
import com.example.screenmanager.model.AlarmRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Alarm koji upravo zvoni.
 *
 * @property snoozeCount koliko je puta ovaj alarm već odložen zaredom.
 * @property wakeCheckRound redni broj wake-up check-a koji je izazvao ovo zvonjenje (0 = nije).
 */
data class ActiveRing(
    val alarm: AlarmRule,
    val reason: RingReason,
    val startedAt: Long,
    val snoozeCount: Int = 0,
    val wakeCheckRound: Int = 0
) {
    /** Probno zvonjenje se ne odlaže — alarm iz forme možda još nije ni sačuvan. */
    val canSnooze: Boolean
        get() = reason != RingReason.TEST && RingPolicy.canSnooze(alarm.snooze, snoozeCount)

    val snoozesLeft: Int
        get() = RingPolicy.snoozesLeft(alarm.snooze, snoozeCount)
}

/**
 * Deljeno stanje zvonjenja unutar procesa: [AlarmRingService] ga postavlja,
 * ekran alarma ga posmatra (i zatvara se kad postane null).
 */
object AlarmRingSession {
    private val _active = MutableStateFlow<ActiveRing?>(null)
    val active: StateFlow<ActiveRing?> = _active.asStateFlow()

    /** Poslednji dodir u misiji — dok je skoro, zvuk je utišan (vidi [RingPolicy.isDucked]). */
    @Volatile
    var lastMissionInteractionAt: Long = 0L
        private set

    fun reportMissionInteraction(now: Long = System.currentTimeMillis()) {
        lastMissionInteractionAt = now
    }

    internal fun start(ring: ActiveRing) {
        lastMissionInteractionAt = 0L
        _active.value = ring
    }

    internal fun clear() {
        lastMissionInteractionAt = 0L
        _active.value = null
    }
}
