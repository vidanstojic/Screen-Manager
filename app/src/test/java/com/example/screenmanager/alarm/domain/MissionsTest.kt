package com.example.screenmanager.alarm.domain

import com.example.screenmanager.model.AlarmMission
import com.example.screenmanager.model.MissionDifficulty
import com.example.screenmanager.model.MissionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MissionsTest {

    /** Ponovo računa izraz iz teksta — proverava da prikazan račun i odgovor odgovaraju. */
    private fun evaluate(text: String): Int {
        val terms = text.replace("−", "-").split(" + ")
        return terms.sumOf { term ->
            when {
                " - " in term -> term.split(" - ").let { it[0].trim().toInt() - it[1].trim().toInt() }
                " × " in term -> term.split(" × ").let { it[0].trim().toInt() * it[1].trim().toInt() }
                else -> term.trim().toInt()
            }
        }
    }

    @Test
    fun `math answers match the shown expression for every difficulty`() {
        val random = Random(1)
        MissionDifficulty.entries.forEach { difficulty ->
            repeat(500) {
                val problem = MathMissions.generate(difficulty, random)
                assertEquals(problem.text, evaluate(problem.text), problem.answer)
                assertTrue("non-negative: ${problem.text}", problem.answer >= 0)
            }
        }
    }

    @Test
    fun `harder levels give bigger numbers`() {
        val random = Random(3)
        fun average(difficulty: MissionDifficulty) =
            List(300) { MathMissions.generate(difficulty, random).answer }.average()
        assertTrue(average(MissionDifficulty.EASY) < average(MissionDifficulty.MEDIUM))
        assertTrue(average(MissionDifficulty.MEDIUM) < average(MissionDifficulty.HARD))
    }

    @Test
    fun `same seed gives the same problems (rotation keeps the current one)`() {
        val first = MathMissions.generate(MissionDifficulty.HARD, 5, Random(99))
        val second = MathMissions.generate(MissionDifficulty.HARD, 5, Random(99))
        assertEquals(first, second)
        assertEquals(5, first.size)
    }

    @Test
    fun `math input ignores spaces and leading zeros`() {
        val problem = MathProblem("2 + 2", 4)
        assertTrue(MathMissions.isCorrect(problem, " 04 "))
        assertFalse(MathMissions.isCorrect(problem, "5"))
        assertFalse(MathMissions.isCorrect(problem, ""))
    }

    @Test
    fun `typing match forgives case, extra spaces and final punctuation`() {
        assertTrue(TypingMissions.matches("I am awake", "  i AM   awake. "))
        assertFalse(TypingMissions.matches("I am awake", "I am asleep"))
        assertFalse(TypingMissions.matches("I am awake", "I am"))
    }

    @Test
    fun `typing progress counts the correctly typed prefix`() {
        assertEquals(5, TypingMissions.correctPrefixLength("I am awake", "i am x"))
        assertEquals(0, TypingMissions.correctPrefixLength("I am awake", "x"))
    }

    @Test
    fun `typing uses distinct phrases or the custom text`() {
        val picked = TypingMissions.pick(AlarmMission(type = MissionType.TYPING, count = 3), Random(5))
        assertEquals(3, picked.toSet().size)
        assertEquals(
            listOf("Rise and shine"),
            TypingMissions.pick(AlarmMission(type = MissionType.TYPING, count = 3, customText = " Rise and shine "), Random(5))
        )
    }

    @Test
    fun `steps progress is capped`() {
        assertEquals(0.5f, StepMissions.progress(15, 30), 1e-6f)
        assertEquals(1f, StepMissions.progress(45, 30), 1e-6f)
        assertTrue(StepMissions.isDone(30, 30))
        assertFalse(StepMissions.isDone(29, 30))
    }

    @Test
    fun `steps mission falls back to math without a sensor`() {
        val steps = AlarmMission(type = MissionType.STEPS)
        assertEquals(MissionType.MATH, effectiveMissionType(steps, stepsAvailable = false))
        assertEquals(MissionType.STEPS, effectiveMissionType(steps, stepsAvailable = true))
        assertEquals(MissionType.TYPING, effectiveMissionType(AlarmMission(type = MissionType.TYPING), false))
    }
}
