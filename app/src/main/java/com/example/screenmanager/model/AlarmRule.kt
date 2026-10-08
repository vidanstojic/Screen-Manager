package com.example.screenmanager.model

import java.io.Serializable

/**
 * Jedan alarm Smart Alarm dela (Phase 2).
 *
 * Osnovna polja (vreme, dani, naziv) + podešavanja po alarmu: zvuk, odlaganje,
 * misija za gašenje, wake-up detox i wake-up check. Svaka grupa ima svoju
 * klasu sa podrazumevanim vrednostima, pa je stari alarm (pre Phase 2)
 * ispravan alarm bez dodatnih opcija.
 *
 * Serializable: forma alarma drži radnu kopiju u `rememberSaveable`.
 *
 * @property repeatDays tokeni "Mon".."Sun"; prazno = alarm zvoni jednom.
 */
data class AlarmRule(
    val id: Long = System.currentTimeMillis(),
    val hour: Int,
    val minute: Int,
    val label: String,
    val repeatDays: Set<String>,
    val enabled: Boolean,
    val sound: AlarmSound = AlarmSound(),
    val snooze: SnoozeConfig = SnoozeConfig(),
    val mission: AlarmMission = AlarmMission(),
    val detox: WakeUpDetox = WakeUpDetox(),
    val wakeCheck: WakeCheckConfig = WakeCheckConfig()
) : Serializable

/**
 * Zvuk alarma.
 *
 * @property toneId `builtin:<ime>` (zvuci aplikacije, res/raw), `system`
 *           (podrazumevani sistemski alarm) ili `custom:<content uri>` (fajl korisnika).
 * @property customName prikazno ime korisnikovog fajla (samo za `custom:`).
 * @property volumePercent jačina sistemskog ALARM toka dok alarm zvoni (posle se vraća).
 * @property fadeInSeconds postepeno pojačavanje od tihog do [volumePercent]; 0 = odmah punom jačinom.
 */
data class AlarmSound(
    val toneId: String = DEFAULT_TONE_ID,
    val customName: String? = null,
    val volumePercent: Int = 80,
    val fadeInSeconds: Int = 30,
    val vibrate: Boolean = true
) : Serializable {
    companion object {
        const val DEFAULT_TONE_ID = "builtin:sunrise"
    }
}

/** Odlaganje ("snooze"): koliko minuta i najviše koliko puta zaredom. */
data class SnoozeConfig(
    val enabled: Boolean = true,
    val minutes: Int = 5,
    val maxCount: Int = 3
) : Serializable

enum class MissionType { NONE, MATH, TYPING, STEPS }

enum class MissionDifficulty { EASY, MEDIUM, HARD }

/**
 * Zadatak koji mora da se reši da bi se alarm ugasio. Dok misija nije
 * rešena, alarm može samo da se odloži (ako je odlaganje uključeno).
 *
 * @property count broj računa (MATH) ili rečenica (TYPING).
 * @property steps broj koraka (STEPS).
 * @property customText ako nije prazno, TYPING traži baš ovaj tekst umesto nasumičnih rečenica.
 */
data class AlarmMission(
    val type: MissionType = MissionType.NONE,
    val difficulty: MissionDifficulty = MissionDifficulty.MEDIUM,
    val count: Int = 3,
    val steps: Int = 30,
    val customText: String = ""
) : Serializable

/**
 * Wake-up detox: posle gašenja alarma izabrane aplikacije i sajtovi su
 * blokirani [minutes] minuta.
 *
 * @property sites domeni ("instagram.com"); blokiraju se i poddomeni.
 */
data class WakeUpDetox(
    val enabled: Boolean = false,
    val apps: List<String> = emptyList(),
    val sites: List<String> = emptyList(),
    val minutes: Int = 30
) : Serializable

/**
 * Wake-up check: [delayMinutes] posle gašenja alarma aplikacija pita "Da li
 * si budan?"; bez potvrde u roku od [WakeCheckConfig.RESPONSE_WINDOW_SECONDS]
 * alarm ponovo zvoni.
 */
data class WakeCheckConfig(
    val enabled: Boolean = false,
    val delayMinutes: Int = 5
) : Serializable {
    companion object {
        const val RESPONSE_WINDOW_SECONDS = 120
        val DELAY_OPTIONS = listOf(5, 10)
    }
}
