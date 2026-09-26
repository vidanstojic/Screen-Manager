package com.example.screenmanager.domain.usage

/**
 * Platformski-nezavisni opis jednog UsageEvents događaja.
 *
 * Android adapter ([UsageEventSource]) prevodi `UsageEvents.Event` u ovaj
 * model, pa je sva logika rekonstrukcije sesija čist Kotlin i testira se
 * bez emulatora.
 */
enum class RawEventKind { RESUMED, PAUSED, SCREEN_OFF, SHUTDOWN }

data class RawUsageEvent(
    val timestamp: Long,
    val packageName: String?,
    val kind: RawEventKind
)

data class UsageSession(
    val packageName: String,
    val start: Long,
    val end: Long
) {
    val durationMs: Long get() = end - start
}

/**
 * @property closedSessions završene sesije.
 * @property openSession sesija koja je još u toku (end = `to`), ili null.
 * @property safeCursor trenutak od kog sledeći sync sme da čita događaje a
 *   da ne izgubi početak otvorene sesije (B5).
 */
data class ReconstructionResult(
    val closedSessions: List<UsageSession>,
    val openSession: UsageSession?,
    val safeCursor: Long
) {
    val allSessions: List<UsageSession>
        get() = if (openSession == null) closedSessions else closedSessions + openSession
}

/**
 * Rekonstruiše foreground sesije iz niza događaja.
 *
 * Model: u svakom trenutku najviše JEDNA aplikacija je u foreground-u.
 * - RESUMED(p) zatvara prethodnu aplikaciju (ako nije p) i otvara p.
 * - PAUSED(p) zatvara p samo ako je p trenutno otvorena.
 * - SCREEN_OFF / SHUTDOWN zatvaraju sve.
 * - Prelaz između aktivnosti iste aplikacije (PAUSED pa RESUMED u roku od
 *   [mergeGapMs]) spaja se u jednu sesiju, da broj sesija bude realan.
 * - Paketi iz [excludedPackages] (launcher, sama aplikacija, systemui) se
 *   ne broje, ali zatvaraju prethodnu sesiju.
 */
class SessionReconstructor(
    private val excludedPackages: Set<String>,
    private val minDurationMs: Long = 1_000L,
    private val mergeGapMs: Long = 5_000L
) {
    fun reconstruct(events: Iterable<RawUsageEvent>, to: Long): ReconstructionResult {
        val closed = ArrayList<UsageSession>()
        var currentPackage: String? = null
        var currentStart = 0L

        fun close(at: Long) {
            val pkg = currentPackage ?: return
            val end = at.coerceAtLeast(currentStart)
            if (end - currentStart >= minDurationMs) {
                closed += UsageSession(pkg, currentStart, end)
            }
            currentPackage = null
        }

        fun open(pkg: String, at: Long) {
            val last = closed.lastOrNull()
            if (last != null && last.packageName == pkg && at - last.end <= mergeGapMs) {
                closed.removeAt(closed.lastIndex)
                currentStart = last.start
            } else {
                currentStart = at
            }
            currentPackage = pkg
        }

        for (event in events) {
            if (event.timestamp > to) break
            when (event.kind) {
                RawEventKind.RESUMED -> {
                    val pkg = event.packageName ?: continue
                    if (pkg == currentPackage) continue
                    close(event.timestamp)
                    if (pkg !in excludedPackages) open(pkg, event.timestamp)
                }

                RawEventKind.PAUSED -> {
                    if (event.packageName != null && event.packageName == currentPackage) {
                        close(event.timestamp)
                    }
                }

                RawEventKind.SCREEN_OFF, RawEventKind.SHUTDOWN -> close(event.timestamp)
            }
        }

        val open = currentPackage?.let { UsageSession(it, currentStart, to.coerceAtLeast(currentStart)) }
        return ReconstructionResult(
            closedSessions = closed,
            openSession = open,
            safeCursor = open?.start ?: to
        )
    }
}

/**
 * Inkrementalno prati koja je aplikacija TRENUTNO u foreground-u.
 *
 * Za razliku od starog pristupa (poslednji RESUMED u zadnjih 15s — B3),
 * stanje se čuva između poziva, pa aplikacija u kojoj korisnik sedi satima
 * bez novih događaja i dalje važi kao foreground.
 */
class ForegroundResolver {
    var currentPackage: String? = null
        private set
    var since: Long = 0L
        private set

    fun apply(event: RawUsageEvent) {
        when (event.kind) {
            RawEventKind.RESUMED -> {
                val pkg = event.packageName ?: return
                if (pkg != currentPackage) {
                    currentPackage = pkg
                    since = event.timestamp
                }
            }

            RawEventKind.PAUSED -> {
                if (event.packageName == currentPackage) currentPackage = null
            }

            RawEventKind.SCREEN_OFF, RawEventKind.SHUTDOWN -> currentPackage = null
        }
    }

    fun reset() {
        currentPackage = null
        since = 0L
    }
}
