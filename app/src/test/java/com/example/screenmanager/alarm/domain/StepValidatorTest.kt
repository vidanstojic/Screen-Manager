package com.example.screenmanager.alarm.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StepValidatorTest {
    private val g = 9.81f

    /**
     * Simulira [seconds] s: akcelerometar (i žiroskop) na 50 Hz, telefon uspravno u ruci
     * (gravitacija duž y), i događaje detektora na svakih [stepMs]. [latencyMs] je
     * kašnjenje detektora: događaj stiže kasnije, ali sa originalnim vremenom koraka.
     */
    private fun simulate(
        seconds: Int,
        stepMs: Long,
        vertical: (Double) -> Double,
        sideways: (Double) -> Double,
        forward: (Double) -> Double = { 0.0 },
        tilt: (Double) -> Double = { 0.0 },
        rotationRate: (Double) -> Double = { 0.0 },
        latencyMs: Long = 0,
        clockOffsetMs: Long = 0,
        stepTimes: List<Long>? = null
    ): Int {
        val validator = StepValidator()
        val steps = ArrayDeque(stepTimes ?: generateSequence(stepMs) { it + stepMs }.takeWhile { it <= seconds * 1_000L }.toList())
        var confirmed = 0
        var t = 0L
        while (t <= seconds * 1_000L + latencyMs) {
            val s = t / 1000.0
            // Naginjanje telefona menja pravac gravitacije (ugao u radijanima oko z ose).
            val angle = tilt(s)
            val gx = g * sin(angle)
            val gy = g * cos(angle)
            validator.onAcceleration(t, (gx + sideways(s)).toFloat(), (gy + vertical(s)).toFloat(), forward(s).toFloat())
            validator.onRotation(t, 0f, 0f, rotationRate(s).toFloat())
            while (steps.isNotEmpty() && t >= steps.first() + latencyMs) confirmed += validator.onStepEvent(steps.removeFirst() + clockOffsetMs)
            t += 20
        }
        return confirmed
    }

    @Test
    fun `walking with the phone in hand counts steps`() {
        // ~1,8 koraka/s: vertikalno poskakivanje 2,5 m/s², blago njihanje levo-desno upola sporije.
        val steps = simulate(
            seconds = 20, stepMs = 550,
            vertical = { s -> 2.5 * sin(2 * PI * s / 0.55) },
            sideways = { s -> 0.8 * sin(2 * PI * s / 1.1) }
        )
        assertTrue("prihvaćeno $steps od ~36 koraka", steps >= 34)
    }

    @Test
    fun `slow sleepy walk with forward sway counts steps`() {
        // BUG-012: spor hod (~1,4 koraka/s), slabo poskakivanje (raspon 1,6 m/s²),
        // jako napred-nazad ubrzanje i njihanje ruke koje rotira telefon.
        val steps = simulate(
            seconds = 20, stepMs = 700,
            vertical = { s -> 0.8 * sin(2 * PI * s / 0.7) },
            sideways = { s -> 0.6 * sin(2 * PI * s / 1.4) },
            forward = { s -> 1.4 * sin(2 * PI * s / 0.7 + 1.0) },
            rotationRate = { s -> 1.2 * sin(2 * PI * s / 1.4) }
        )
        assertTrue("prihvaćeno $steps od ~28 koraka", steps >= 25)
    }

    @Test
    fun `gentle walk with the phone held in front counts steps`() {
        // BUG-012 (S25 FE): lagan hod, telefon ispred sebe — poskakivanje samo 0,9 m/s².
        val steps = simulate(
            seconds = 20, stepMs = 600,
            vertical = { s -> 0.45 * sin(2 * PI * s / 0.6) },
            sideways = { s -> 0.5 * sin(2 * PI * s / 1.2) },
            forward = { s -> 1.0 * sin(2 * PI * s / 0.6 + 1.0) },
            rotationRate = { s -> 0.8 * sin(2 * PI * s / 1.2) }
        )
        assertTrue("prihvaćeno $steps od ~33 koraka", steps >= 31)
    }

    @Test
    fun `detector on a different clock is checked against recent motion`() {
        val steps = simulate(
            seconds = 20, stepMs = 550,
            vertical = { s -> 2.0 * sin(2 * PI * s / 0.55) },
            sideways = { 0.0 },
            clockOffsetMs = 60_000
        )
        assertTrue("prihvaćeno $steps od ~36 koraka", steps >= 34)
    }

    @Test
    fun `late detector events are still validated`() {
        // BUG-012: Samsung prijavljuje korake sa zakašnjenjem od par sekundi.
        val steps = simulate(
            seconds = 20, stepMs = 550,
            vertical = { s -> 2.0 * sin(2 * PI * s / 0.55) },
            sideways = { 0.0 },
            latencyMs = 3_000
        )
        assertTrue("prihvaćeno $steps od ~36 koraka", steps >= 34)
    }

    @Test
    fun `one irregular step does not drop the streak`() {
        // Okret u sobi: jedan korak prebrzo za prethodnim, ostali normalni.
        val times = mutableListOf<Long>()
        var t = 0L
        repeat(20) { i ->
            t += if (i == 8) 200 else 600
            times += t
        }
        val steps = simulate(
            seconds = 13, stepMs = 600,
            vertical = { s -> 2.0 * sin(2 * PI * s / 0.6) },
            sideways = { 0.0 },
            stepTimes = times
        )
        assertEquals(19, steps)
    }

    @Test
    fun `shaking the phone left and right is not walking`() {
        // BUG-001: ritmično njihanje u ritmu hoda, jako horizontalno, skoro bez vertikale.
        val steps = simulate(
            seconds = 20, stepMs = 550,
            vertical = { s -> 0.4 * sin(2 * PI * s / 0.55) },
            sideways = { s -> 10.0 * sin(2 * PI * s / 1.1) }
        )
        assertEquals(0, steps)
    }

    @Test
    fun `jerking the phone forward and back is not walking`() {
        // BUG-012: trzanje ka napred — horizontalno, bez poskakivanja.
        val steps = simulate(
            seconds = 20, stepMs = 550,
            vertical = { 0.0 },
            sideways = { 0.0 },
            forward = { s -> 8.0 * sin(2 * PI * s / 0.55) }
        )
        assertEquals(0, steps)
    }

    @Test
    fun `tilting the phone back and forth is not walking`() {
        val steps = simulate(
            seconds = 20, stepMs = 550,
            vertical = { 0.0 },
            sideways = { 0.0 },
            tilt = { s -> 0.8 * sin(2 * PI * s / 1.1) },
            rotationRate = { s -> 4.6 * cos(2 * PI * s / 1.1) }
        )
        assertEquals(0, steps)
    }

    @Test
    fun `waving the phone up and down while rotating it is not walking`() {
        // Mahanje iz ručnog zgloba: ima vertikale, ali telefon se jako vrti.
        val steps = simulate(
            seconds = 20, stepMs = 550,
            vertical = { s -> 4.0 * sin(2 * PI * s / 0.55) },
            sideways = { 0.0 },
            rotationRate = { s -> 6.0 * sin(2 * PI * s / 0.55) }
        )
        assertEquals(0, steps)
    }

    @Test
    fun `fast shaking is faster than any walk`() {
        // Tresenje gore-dole 5 puta u sekundi: vertikalno jeste, ali ritam nije hod.
        val steps = simulate(
            seconds = 10, stepMs = 200,
            vertical = { s -> 8.0 * sin(2 * PI * s / 0.2) },
            sideways = { 0.0 }
        )
        assertEquals(0, steps)
    }

    @Test
    fun `isolated detector events are never confirmed`() {
        val steps = simulate(
            seconds = 30, stepMs = 3_000,
            vertical = { s -> 2.5 * sin(2 * PI * s / 0.55) },
            sideways = { 0.0 }
        )
        assertEquals(0, steps)
    }

    @Test
    fun `steps are credited only after a streak, including the pending ones`() {
        val validator = StepValidator()
        var t = 0L
        val results = mutableListOf<Int>()
        while (t <= 3_000) {
            validator.onAcceleration(t, 0f, (g + 2.5 * sin(2 * PI * t / 550.0)).toFloat(), 0f)
            if (t > 0 && t % 540 == 0L) results += validator.onStepEvent(t)
            t += 20
        }
        // Prvi čeka, drugi potvrđuje oba, dalje po jedan.
        assertEquals(listOf(0, 2, 1, 1, 1), results)
    }

    @Test
    fun `without accelerometer data the detector is trusted after a streak`() {
        val validator = StepValidator()
        val results = (1..5).map { validator.onStepEvent(it * 550L) }
        assertEquals(listOf(0, 2, 1, 1, 1), results)
    }
}
