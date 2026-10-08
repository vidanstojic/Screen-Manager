package com.example.screenmanager.alarm.domain

import com.example.screenmanager.model.AlarmMission
import com.example.screenmanager.model.MissionDifficulty
import com.example.screenmanager.model.MissionType
import kotlin.random.Random

/** Jedan račun misije: tekst za prikaz ("7 × 14 + 23") i tačan rezultat. */
data class MathProblem(val text: String, val answer: Int)

/**
 * Računi za buđenje. Dovoljno teški da zahtevaju budnu glavu, ali bez
 * deljenja i negativnih rezultata (kuca se na numeričkoj tastaturi).
 *
 * - EASY:   dvocifreno ± dvocifreno            (npr. 47 + 38)
 * - MEDIUM: jednocifreno × dvocifreno + dvocifreno (npr. 7 × 14 + 23)
 * - HARD:   dva množenja i sabiranje            (npr. 13 × 7 + 16 × 8)
 */
object MathMissions {

    fun generate(difficulty: MissionDifficulty, random: Random): MathProblem = when (difficulty) {
        MissionDifficulty.EASY -> {
            val a = random.nextInt(20, 90)
            val b = random.nextInt(10, 60)
            if (random.nextBoolean() && a > b) MathProblem("$a − $b", a - b) else MathProblem("$a + $b", a + b)
        }
        MissionDifficulty.MEDIUM -> {
            val a = random.nextInt(3, 10)
            val b = random.nextInt(11, 20)
            val c = random.nextInt(10, 100)
            MathProblem("$a × $b + $c", a * b + c)
        }
        MissionDifficulty.HARD -> {
            val a = random.nextInt(12, 30)
            val b = random.nextInt(3, 10)
            val c = random.nextInt(11, 20)
            val d = random.nextInt(3, 10)
            MathProblem("$a × $b + $c × $d", a * b + c * d)
        }
    }

    fun generate(difficulty: MissionDifficulty, count: Int, random: Random): List<MathProblem> =
        List(count.coerceIn(1, MAX_COUNT)) { generate(difficulty, random) }

    /** Unos se poredi kao broj (razmaci i vodeće nule se ignorišu). */
    fun isCorrect(problem: MathProblem, input: String): Boolean =
        input.filterNot { it.isWhitespace() }.toIntOrNull() == problem.answer

    const val MAX_COUNT = 10
}

/**
 * Prekucavanje rečenica. Rečenice su dovoljno dugačke da se ne mogu
 * otkucati u polusnu, a poređenje prašta velika/mala slova i višak razmaka.
 */
object TypingMissions {

    val phrases: List<String> = listOf(
        "I am awake and ready to start the day",
        "Today I will focus on what matters most",
        "My phone can wait until I am fully awake",
        "A calm morning makes a productive day",
        "I choose to get up instead of scrolling",
        "Small steps every morning build big habits",
        "Fresh air and water come before social media",
        "I will make my bed before I check messages",
        "The best way to wake up is to stand up",
        "Good mornings start with a clear head"
    )

    /** [count] različitih rečenica (ili korisnikov tekst, ako ga je zadao). */
    fun pick(mission: AlarmMission, random: Random): List<String> {
        val custom = mission.customText.trim()
        if (custom.isNotEmpty()) return listOf(custom)
        return phrases.shuffled(random).take(mission.count.coerceIn(1, phrases.size))
    }

    fun matches(expected: String, typed: String): Boolean = normalize(expected) == normalize(typed)

    /** Koliko je početnih znakova već tačno otkucano — za isticanje napretka. */
    fun correctPrefixLength(expected: String, typed: String): Int {
        val target = expected.lowercase()
        val input = typed.lowercase()
        var index = 0
        while (index < input.length && index < target.length && input[index] == target[index]) index++
        return index
    }

    private fun normalize(text: String): String =
        text.trim().replace(WHITESPACE, " ").lowercase().trimEnd('.', '!', '?')

    private val WHITESPACE = Regex("\\s+")
}

/** Napredak misije sa koracima. */
object StepMissions {
    const val MIN_STEPS = 10
    const val MAX_STEPS = 300

    fun progress(steps: Int, goal: Int): Float = (steps.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f)

    fun isDone(steps: Int, goal: Int): Boolean = steps >= goal.coerceAtLeast(1)
}

/**
 * Misija koja će se stvarno igrati. Koraci zahtevaju senzor i dozvolu za
 * fizičku aktivnost; bez njih korisnik dobija račun (alarm ne sme da
 * postane nemoguć za gašenje).
 */
fun effectiveMissionType(mission: AlarmMission, stepsAvailable: Boolean): MissionType =
    if (mission.type == MissionType.STEPS && !stepsAvailable) MissionType.MATH else mission.type
