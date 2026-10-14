package com.example.screenmanager.domain.wakeup

/**
 * Stanje detekcije buđenja (čuva se u app_internal_state, vidi RuntimeStateRepository).
 *
 * @property darkSince kraj poslednjeg PRAVOG korišćenja telefona — od tada teče "noć".
 *   Kratka paljenja ekrana (sat, notifikacija) ga ne pomeraju.
 * @property screenOnAt početak tekućeg perioda upaljenog ekrana, null dok je ekran ugašen.
 * @property wakeAt prvo paljenje ekrana posle dovoljno duge noći (kandidat za buđenje).
 * @property wakeOnMs koliko je ekran bio upaljen od [wakeAt], ne računajući tekući period.
 */
data class WakeState(
    val darkSince: Long? = null,
    val screenOnAt: Long? = null,
    val wakeAt: Long? = null,
    val wakeOnMs: Long = 0L
)

/**
 * Detekcija buđenja za jutarnju blokadu (TRS 2.5), čista logika bez Android zavisnosti.
 *
 * Ranije (BUG-004) je blokada okidala samo ako je PRVO paljenje ekrana posle noći
 * trajalo neprekidno [triggerDelayMinutes] uz aplikaciju u prvom planu. Svako kratko
 * paljenje (sat, notifikacija, alarm koji se ugasi pa zaključa telefon) trošilo je
 * to jutro, jer je pomeralo početak "noći" na trenutak gašenja ekrana.
 *
 * Sada:
 * 1. Noć se meri od kraja poslednjeg PRAVOG korišćenja ([darkSince]): period upaljenog
 *    ekrana kraći od [REAL_USE_MS] je pogled, ne korišćenje, i ne pomera je.
 * 2. Prvo paljenje ekrana posle `inactivityHours` noći je kandidat za buđenje.
 * 3. Blokada se pali kad se od kandidata nakupi `triggerDelayMinutes` upaljenog ekrana,
 *    i preko više kratkih paljenja (ugasiš alarm, zaključaš, pa uzmeš telefon).
 * 4. Kandidat stariji od [WAKE_WINDOW_MS] zastareva (npr. pogled u 4 h), a sledeće
 *    paljenje ekrana posle iste noći postaje novi kandidat.
 */
object WakeUpDetector {
    /** Upaljen ekran duži od ovoga = korisnik je stvarno koristio telefon. */
    const val REAL_USE_MS: Long = 90_000L

    /** Kandidat za buđenje važi najduže ovoliko. */
    const val WAKE_WINDOW_MS: Long = 3 * 60 * 60_000L

    private const val HOUR_MS = 60 * 60_000L
    private const val MINUTE_MS = 60_000L

    fun onScreenOn(state: WakeState, now: Long, inactivityHours: Int): WakeState {
        var next = state.copy(screenOnAt = now)
        val dark = next.darkSince ?: return next
        if (next.wakeAt != null && now - next.wakeAt!! > WAKE_WINDOW_MS) {
            next = next.copy(wakeAt = null, wakeOnMs = 0L)
        }
        if (next.wakeAt == null && now - dark >= inactivityHours.coerceAtLeast(1) * HOUR_MS) {
            next = next.copy(wakeAt = now, wakeOnMs = 0L)
        }
        return next
    }

    fun onScreenOff(state: WakeState, now: Long): WakeState {
        val onAt = state.screenOnAt ?: return if (state.darkSince == null) state.copy(darkSince = now) else state
        val period = (now - onAt).coerceAtLeast(0L)
        val off = state.copy(screenOnAt = null)
        return when {
            // Kandidat čeka: noć se ne pomera, upaljeno vreme se sabira.
            off.wakeAt != null -> off.copy(wakeOnMs = off.wakeOnMs + period)
            period >= REAL_USE_MS || off.darkSince == null -> off.copy(darkSince = now)
            else -> off
        }
    }

    /** Koliko još upaljenog ekrana do blokade; null ako nema kandidata ili je ekran ugašen. */
    fun remainingMs(state: WakeState, now: Long, triggerDelayMinutes: Int): Long? {
        val wakeAt = state.wakeAt ?: return null
        val onAt = state.screenOnAt ?: return null
        if (now - wakeAt > WAKE_WINDOW_MS) return null
        val used = state.wakeOnMs + (now - onAt).coerceAtLeast(0L)
        return (triggerDelayMinutes.coerceAtLeast(0) * MINUTE_MS - used).coerceAtLeast(0L)
    }

    fun shouldTrigger(state: WakeState, now: Long, triggerDelayMinutes: Int): Boolean =
        remainingMs(state, now, triggerDelayMinutes) == 0L

    /** Blokada je upaljena: kandidat je potrošen, sledeća noć počinje od narednog gašenja ekrana. */
    fun onTriggered(state: WakeState): WakeState = state.copy(wakeAt = null, wakeOnMs = 0L, darkSince = null)
}
