package com.example.screenmanager.alarm.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class StepValidatorTest {
    private val g = 9.81f

    /**
     * Simulira [seconds] s: akcelerometar na 50 Hz (telefon uspravno u ruci, gravitacija duž y)
     * i događaje detektora na svakih [stepMs].
     */
    private fun simulate(
        seconds: Int,
        stepMs: Long,
        vertical: (Double) -> Double,
        sideways: (Double) -> Double,
        tilt: (Double) -> Double = { 0.0 }
    ): Int {
        val validator = StepValidator()
        var confirmed = 0
        var nextStep = stepMs
        var t = 0L
        while (t <= seconds * 1_000L) {
            val s = t / 1000.0
            // Naginjanje telefona menja pravac gravitacije (ugao u radijanima oko z ose).
            val angle = tilt(s)
            val gx = g * sin(angle)
            val gy = g * cos(angle)
            validator.onAcceleration(t, (gx + sideways(s)).toFloat(), (gy + vertical(s)).toFloat(), 0f)
            if (t >= nextStep) {
                confirmed += validator.onStepEvent(t)
                nextStep += stepMs
            }
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
        assertTrue("prihvaćeno $steps od ~36 koraka", steps >= 30)
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
    fun `tilting the phone back and forth is not walking`() {
        val steps = simulate(
            seconds = 20, stepMs = 550,
            vertical = { 0.0 },
            sideways = { 0.0 },
            tilt = { s -> 0.8 * sin(2 * PI * s / 1.1) }
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
        // Prva tri čekaju, četvrti potvrđuje sva četiri, dalje po jedan.
        assertEquals(listOf(0, 0, 0, 4, 1), results)
    }

    @Test
    fun `without accelerometer data the detector is trusted after a streak`() {
        val validator = StepValidator()
        val results = (1..5).map { validator.onStepEvent(it * 550L) }
        assertEquals(listOf(0, 0, 0, 4, 1), results)
    }
}
