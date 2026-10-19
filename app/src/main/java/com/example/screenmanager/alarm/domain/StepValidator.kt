package com.example.screenmanager.alarm.domain

import kotlin.math.sqrt

/**
 * Provera da su koraci iz [android.hardware.Sensor.TYPE_STEP_DETECTOR] stvarni hod
 * (BUG-001: detektor broji i ritmično njihanje telefona; BUG-012: prva verzija je
 * odbijala i prave korake).
 *
 * Čista logika: servis prosleđuje uzorke akcelerometra, žiroskopa i događaje
 * detektora, a dobija broj POTVRĐENIH koraka.
 *
 * Korak je dobar ako u prozoru oko njega:
 * 1. ukupno ubrzanje (|a|, ne zavisi od položaja telefona) "poskakuje" bar
 *    [MIN_BOUNCE_RANGE] — hod odbija telefon gore-dole, a naginjanje telefona ne
 *    menja |a|;
 * 2. vertikala nije zanemarljiva u odnosu na horizontalu (njihanje levo-desno ili
 *    trzanje napred-nazad je skoro čisto horizontalno);
 * 3. telefon se ne vrti jako (žiroskop, RMS ≤ [MAX_ROTATION_RMS]) — mahanje i
 *    naginjanje rotiraju telefon, a u hodu ga ruka drži relativno mirno;
 * 4. razmak od prethodnog koraka je u [MIN_INTERVAL_MS, MAX_INTERVAL_MS].
 *
 * Tolerancija (BUG-012): koraci se priznaju posle [CONFIRM_STREAK] dobrih zaredom;
 * jedan loš korak (okret u sobi, neravan korak) se samo ne broji, a niz se prekida
 * tek posle [MAX_MISSES] loša zaredom ili pauze duže od [MAX_INTERVAL_MS].
 * Bafer od [BUFFER_MS] pokriva kašnjenje detektora (Samsung prijavljuje korake sa
 * zakašnjenjem, sa originalnim vremenom) — ranije su takvi koraci padali jer su
 * uzorci akcelerometra iz njihovog prozora već bili obrisani.
 *
 * Vremena su u milisekundama, u istoj vremenskoj osi za sve senzore.
 */
class StepValidator(private val diagnostics: ((String) -> Unit)? = null) {
    private class Sample(val time: Long, val bounce: Float, val vertical: Float, val horizontal: Float)
    private class Rotation(val time: Long, val rate: Float)

    private val samples = ArrayDeque<Sample>()
    private val rotations = ArrayDeque<Rotation>()
    private var gravity: FloatArray? = null
    private var meanMagnitude: Float? = null
    private var lastAccelerationAt: Long? = null
    private var lastStepAt: Long? = null
    private var streak = 0
    private var misses = 0
    private var pending = 0

    fun onAcceleration(time: Long, x: Float, y: Float, z: Float) {
        val dt = lastAccelerationAt?.let { (time - it).coerceIn(1L, MAX_SAMPLE_GAP_MS) } ?: 0L
        lastAccelerationAt = time
        // Vremenska konstanta ne zavisi od frekvencije senzora (Samsung GAME ≈ 100 Hz).
        val alpha = GRAVITY_TAU_MS / (GRAVITY_TAU_MS + dt)
        val g = gravity?.also {
            it[0] = alpha * it[0] + (1 - alpha) * x
            it[1] = alpha * it[1] + (1 - alpha) * y
            it[2] = alpha * it[2] + (1 - alpha) * z
        } ?: floatArrayOf(x, y, z).also { gravity = it }

        val gNorm = sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2]).takeIf { it > 1e-3f } ?: return
        val lx = x - g[0]
        val ly = y - g[1]
        val lz = z - g[2]
        val vertical = (lx * g[0] + ly * g[1] + lz * g[2]) / gNorm
        val hx = lx - vertical * g[0] / gNorm
        val hy = ly - vertical * g[1] / gNorm
        val hz = lz - vertical * g[2] / gNorm
        // |a| prema svom proseku: naginjanje ne menja |a|, a filtrirani vektor bi se pri
        // okretanju "skratio" i pravio lažno poskakivanje.
        val magnitude = sqrt(x * x + y * y + z * z)
        val mean = (meanMagnitude?.let { alpha * it + (1 - alpha) * magnitude } ?: magnitude).also { meanMagnitude = it }
        val bounce = magnitude - mean
        samples.addLast(Sample(time, bounce, vertical, sqrt(hx * hx + hy * hy + hz * hz)))
        while (samples.isNotEmpty() && time - samples.first().time > BUFFER_MS) samples.removeFirst()
    }

    /** Uzorak žiroskopa (rad/s). */
    fun onRotation(time: Long, x: Float, y: Float, z: Float) {
        rotations.addLast(Rotation(time, sqrt(x * x + y * y + z * z)))
        while (rotations.isNotEmpty() && time - rotations.first().time > BUFFER_MS) rotations.removeFirst()
    }

    /** Događaj detektora koraka u trenutku [time]; vraća broj novo potvrđenih koraka. */
    fun onStepEvent(time: Long): Int {
        val interval = lastStepAt?.let { time - it }
        lastStepAt = time
        // Pauza u hodu: novi niz, a ovaj korak može biti njegov prvi.
        if (interval != null && interval > MAX_INTERVAL_MS) resetStreak()

        val good = (interval == null || interval >= MIN_INTERVAL_MS) && looksLikeWalking(time, interval)
        if (!good) {
            misses++
            if (misses >= MAX_MISSES) resetStreak()
            return 0
        }
        misses = 0
        streak++
        pending++
        if (streak < CONFIRM_STREAK) return 0
        return pending.also { pending = 0 }
    }

    private fun resetStreak() {
        streak = 0
        misses = 0
        pending = 0
    }

    private fun looksLikeWalking(time: Long, interval: Long?): Boolean {
        var from = time - WINDOW_BEFORE_MS
        var to = time + WINDOW_AFTER_MS
        var window = samples.filter { it.time in from..to }
        if (window.size < MIN_SAMPLES && samples.isNotEmpty()) {
            // Vreme događaja ne pada u bafer (drugačiji sat detektora ili veliko kašnjenje):
            // proverava se poslednje kretanje umesto da se korak odbaci.
            to = samples.last().time
            from = to - WINDOW_BEFORE_MS
            window = samples.filter { it.time in from..to }
        }
        if (window.size < MIN_SAMPLES) {
            // Bez akcelerometra verujemo detektoru; inače nema podataka za proveru.
            diagnostics?.invoke("step t=$time interval=$interval samples=${window.size} → ${samples.isEmpty()}")
            return samples.isEmpty()
        }
        val bounceRange = window.maxOf { it.bounce } - window.minOf { it.bounce }
        val verticalRms = rms(window) { it.vertical }
        val horizontalRms = rms(window) { it.horizontal }
        val turning = rotations.filter { it.time in from..to }
        val rotationRms = if (turning.isEmpty()) 0.0 else sqrt(turning.sumOf { (it.rate * it.rate).toDouble() } / turning.size)

        val ok = bounceRange >= MIN_BOUNCE_RANGE &&
            verticalRms >= MIN_VERTICAL_SHARE * horizontalRms &&
            rotationRms <= MAX_ROTATION_RMS
        diagnostics?.invoke(
            "step t=$time interval=$interval bounce=%.2f vRms=%.2f hRms=%.2f rot=%.2f → $ok"
                .format(bounceRange, verticalRms, horizontalRms, rotationRms)
        )
        return ok
    }

    private inline fun rms(window: List<Sample>, value: (Sample) -> Float): Double =
        sqrt(window.sumOf { value(it).let { v -> (v * v).toDouble() } } / window.size)

    companion object {
        /** Vremenska konstanta niskopropusnog filtera za gravitaciju. */
        const val GRAVITY_TAU_MS = 1_000f
        const val MAX_SAMPLE_GAP_MS = 200L

        /** Pokriva i zakašnjele događaje detektora. */
        const val BUFFER_MS = 10_000L
        const val WINDOW_BEFORE_MS = 1_500L
        const val WINDOW_AFTER_MS = 300L
        const val MIN_SAMPLES = 10

        /**
         * Raspon |a| − g (m/s²). Namerno nisko (BUG-012, test na S25 FE): i lagan hod sa
         * telefonom ispred sebe ga prelazi, a naginjanje telefona ne menja |a| uopšte.
         */
        const val MIN_BOUNCE_RANGE = 0.8f

        /** Vertikalni RMS mora biti bar ovoliki deo horizontalnog (njihanje je ~0,05). */
        const val MIN_VERTICAL_SHARE = 0.2

        /** RMS ugaone brzine (rad/s); hod i njihanje ruke su ispod, mahanje iz zgloba iznad. */
        const val MAX_ROTATION_RMS = 3.5

        /** ~4 koraka/s je granica; brže je tresenje. */
        const val MIN_INTERVAL_MS = 250L
        const val MAX_INTERVAL_MS = 2_000L

        const val CONFIRM_STREAK = 2
        const val MAX_MISSES = 3
    }
}
