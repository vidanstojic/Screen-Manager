package com.example.screenmanager.alarm.domain

import com.example.screenmanager.model.SnoozeConfig

/** Zašto alarm zvoni — utiče na to šta se dešava posle gašenja. */
enum class RingReason {
    /** Redovan termin alarma. */
    SCHEDULED,

    /** Isteklo odlaganje. */
    SNOOZE,

    /** Korisnik nije potvrdio wake-up check na vreme. */
    WAKE_CHECK,

    /** Probno zvonjenje iz forme alarma. */
    TEST
}

/** Pravila zvonjenja bez Android zavisnosti (testirana u RingPolicyTest). */
object RingPolicy {

    /** Posle ovoliko zvonjenja bez reakcije alarm se sam odlaže (ili utišava ako odlaganje nije moguće). */
    const val AUTO_SILENCE_MS = 10 * 60_000L

    /** Početna jačina pri postepenom pojačavanju (deo ciljne jačine). */
    const val FADE_START_FRACTION = 0.1f

    /** Jačina dok korisnik rešava misiju (deo ciljne jačine). */
    const val DUCKED_FRACTION = 0.15f

    /** Koliko dugo posle poslednjeg dodira u misiji zvuk ostaje utišan. */
    const val DUCK_HOLD_MS = 30_000L

    fun canSnooze(config: SnoozeConfig, snoozesUsed: Int): Boolean =
        config.enabled && config.maxCount > 0 && snoozesUsed < config.maxCount

    fun snoozesLeft(config: SnoozeConfig, snoozesUsed: Int): Int =
        if (!config.enabled) 0 else (config.maxCount - snoozesUsed).coerceAtLeast(0)

    fun snoozeUntil(now: Long, config: SnoozeConfig): Long = now + config.minutes.coerceAtLeast(1) * 60_000L

    /**
     * Deo ciljne jačine posle [elapsedMs] zvonjenja: linearno od
     * [FADE_START_FRACTION] do 1 za [fadeInSeconds]; 0 sekundi = odmah 1.
     */
    fun volumeFraction(elapsedMs: Long, fadeInSeconds: Int): Float {
        if (fadeInSeconds <= 0) return 1f
        val progress = (elapsedMs.toFloat() / (fadeInSeconds * 1_000f)).coerceIn(0f, 1f)
        return FADE_START_FRACTION + (1f - FADE_START_FRACTION) * progress
    }

    /** Da li je zvuk utišan jer korisnik upravo rešava misiju. */
    fun isDucked(lastMissionInteractionAt: Long, now: Long): Boolean =
        lastMissionInteractionAt > 0 && now - lastMissionInteractionAt < DUCK_HOLD_MS

    /** Jačina sistemskog ALARM toka (indeks) za [percent] od [maxIndex]; nikad 0 — alarm mora da se čuje. */
    fun streamVolumeIndex(percent: Int, maxIndex: Int): Int =
        ((percent.coerceIn(0, 100) * maxIndex + 50) / 100).coerceIn(1, maxIndex.coerceAtLeast(1))
}
