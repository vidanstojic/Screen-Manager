package com.example.screenmanager.alarm.domain

import kotlin.math.sqrt

/**
 * Provera da su koraci iz [android.hardware.Sensor.TYPE_STEP_DETECTOR] stvarni hod
 * (BUG-001: hardverski detektor broji i ritmično njihanje telefona levo-desno).
 *
 * Čista logika: servis prosleđuje uzorke akcelerometra i događaje detektora, a
 * dobija broj POTVRĐENIH koraka.
 *
 * Korak se prihvata ako u prozoru oko njega:
 * 1. postoji vertikalno poskakivanje (komponenta ubrzanja duž gravitacije, raspon
 *    ≥ [MIN_VERTICAL_RANGE]) — hod "odbija" telefon gore-dole;
 * 2. vertikalno kretanje nije zanemarljivo u odnosu na horizontalno (njihanje
 *    levo-desno je skoro čisto horizontalno);
 * 3. ritam liči na hod: razmak od prethodnog koraka u [MIN_INTERVAL_MS, MAX_INTERVAL_MS]
 *    i bez naglih skokova tempa.
 * Koraci se priznaju tek kad [CONFIRM_STREAK] uzastopnih prođe proveru (pa i oni
 * na čekanju), a svaki loš korak poništava niz.
 *
 * Vremena su u milisekundama, u istoj vremenskoj osi za oba senzora.
 */
class StepValidator {
    private class Sample(val time: Long, val vertical: Float, val horizontal: Float)

    private val samples = ArrayDeque<Sample>()
    private var gravity: FloatArray? = null
    private var lastStepAt: Long? = null
    private var lastInterval: Long? = null
    private var streak = 0
    private var pending = 0

    fun onAcceleration(time: Long, x: Float, y: Float, z: Float) {
        val g = gravity?.also {
            it[0] = GRAVITY_ALPHA * it[0] + (1 - GRAVITY_ALPHA) * x
            it[1] = GRAVITY_ALPHA * it[1] + (1 - GRAVITY_ALPHA) * y
            it[2] = GRAVITY_ALPHA * it[2] + (1 - GRAVITY_ALPHA) * z
        } ?: floatArrayOf(x, y, z).also { gravity = it }

        val norm = sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2]).takeIf { it > 1e-3f } ?: return
        val lx = x - g[0]
        val ly = y - g[1]
        val lz = z - g[2]
        val vertical = (lx * g[0] + ly * g[1] + lz * g[2]) / norm
        val hx = lx - vertical * g[0] / norm
        val hy = ly - vertical * g[1] / norm
        val hz = lz - vertical * g[2] / norm
        samples.addLast(Sample(time, vertical, sqrt(hx * hx + hy * hy + hz * hz)))
        while (samples.isNotEmpty() && time - samples.first().time > BUFFER_MS) samples.removeFirst()
    }

    /** Događaj detektora koraka u trenutku [time]; vraća broj novo potvrđenih koraka. */
    fun onStepEvent(time: Long): Int {
        val interval = lastStepAt?.let { time - it }
        lastStepAt = time
        if (!looksLikeWalking(time) || !rhythmOk(interval)) {
            streak = 0
            pending = 0
            lastInterval = null
            return 0
        }
        lastInterval = interval
        streak++
        pending++
        if (streak < CONFIRM_STREAK) return 0
        return pending.also { pending = 0 }
    }

    private fun rhythmOk(interval: Long?): Boolean {
        if (interval == null) return true // prvi korak niza
        if (interval !in MIN_INTERVAL_MS..MAX_INTERVAL_MS) return false
        val previous = lastInterval ?: return true
        val ratio = interval.toFloat() / previous
        return ratio in (1 / MAX_TEMPO_CHANGE)..MAX_TEMPO_CHANGE
    }

    private fun looksLikeWalking(time: Long): Boolean {
        val window = samples.filter { it.time in (time - WINDOW_BEFORE_MS)..(time + WINDOW_AFTER_MS) }
        if (window.size < MIN_SAMPLES) return samples.isEmpty() // bez akcelerometra verujemo detektoru
        val verticalRange = window.maxOf { it.vertical } - window.minOf { it.vertical }
        val verticalRms = sqrt(window.sumOf { (it.vertical * it.vertical).toDouble() } / window.size)
        val horizontalRms = sqrt(window.sumOf { (it.horizontal * it.horizontal).toDouble() } / window.size)
        return verticalRange >= MIN_VERTICAL_RANGE && verticalRms >= MIN_VERTICAL_SHARE * horizontalRms
    }

    companion object {
        /** Niskopropusni filter za gravitaciju (~0,5 s na 50 Hz). */
        const val GRAVITY_ALPHA = 0.9f
        const val BUFFER_MS = 3_000L
        const val WINDOW_BEFORE_MS = 1_200L
        const val WINDOW_AFTER_MS = 200L
        const val MIN_SAMPLES = 10

        /** Raspon vertikalnog ubrzanja (m/s²) koji hod sigurno pravi. */
        const val MIN_VERTICAL_RANGE = 2.0f

        /** Vertikalni RMS mora biti bar ovoliki deo horizontalnog. */
        const val MIN_VERTICAL_SHARE = 0.6

        /** ~3 koraka/s je najbrži hod; brže je tresenje. */
        const val MIN_INTERVAL_MS = 330L
        const val MAX_INTERVAL_MS = 1_500L
        const val MAX_TEMPO_CHANGE = 1.6f

        const val CONFIRM_STREAK = 4
    }
}
