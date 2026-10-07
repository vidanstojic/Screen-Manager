package com.example.screenmanager.domain

import android.content.Context
import android.util.Log
import com.example.screenmanager.data.local.AppInternalState
import com.example.screenmanager.data.local.AppInternalStateDao
import com.example.screenmanager.data.local.AppUsageLog
import com.example.screenmanager.data.local.AppUsageLogDao
import com.example.screenmanager.data.local.DailyUsage
import com.example.screenmanager.data.local.HourlyUsage
import com.example.screenmanager.data.local.UsageHourlyDao
import com.example.screenmanager.data.local.UsageHourlyEntity
import com.example.screenmanager.data.local.UsageSummary
import com.example.screenmanager.data.usage.UsageEventSource
import com.example.screenmanager.domain.usage.SessionReconstructor
import com.example.screenmanager.domain.usage.UsageSession
import com.example.screenmanager.domain.usage.UsageSplitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Izvor istine za potrošnju.
 *
 * Tok podataka: UsageEvents → [SessionReconstructor] → app_usage_log (sirove
 * sesije, idempotentno) → [UsageSplitter] → usage_hourly (rollup) → UI.
 *
 * Sync je jedini upisivač (ranije je i FocusMonitorService upisivao iste
 * sesije → duplo brojanje, B4).
 */
class UsageStatsRepository(
    context: Context,
    private val usageLogDao: AppUsageLogDao,
    private val hourlyDao: UsageHourlyDao,
    private val stateDao: AppInternalStateDao
) {
    private val eventSource = UsageEventSource(context)
    private val reconstructor by lazy { SessionReconstructor(eventSource.excludedPackages) }

    // --- Sve aplikacije zbirno ---

    fun observeTodayHourlyUsage(): Flow<List<HourlyUsage>> =
        hourlyDao.observeHourly(TimeBuckets.epochDay(System.currentTimeMillis()))

    /** Po satu za izabrani dan (DayPicker). */
    fun observeHourlyUsageForDay(dayStartTimestamp: Long): Flow<List<HourlyUsage>> =
        hourlyDao.observeHourly(TimeBuckets.epochDay(dayStartTimestamp))

    /** Po aplikaciji za izabrani dan — lista aplikacija prati DayPicker. */
    fun observeTotalsForDay(dayStartTimestamp: Long): Flow<List<UsageSummary>> {
        val day = TimeBuckets.epochDay(dayStartTimestamp)
        return hourlyDao.observeTotals(day, day)
    }

    fun observeDailyTotals(): Flow<List<UsageSummary>> = observeTotalsForDay(System.currentTimeMillis())

    fun observeWeeklyTotals(): Flow<List<UsageSummary>> = totalsSince(TimeBuckets.startOfWeek())

    /**
     * Po aplikaciji za poslednjih 7 dana (danas uključen) — isti prozor kao
     * [observeWeeklyDailyBreakdown], pa se lista i "Week" grafik slažu.
     */
    fun observeRollingWeekTotals(): Flow<List<UsageSummary>> {
        val today = TimeBuckets.epochDay(System.currentTimeMillis())
        return hourlyDao.observeTotals(today - 6, today)
    }

    fun observeMonthlyTotals(): Flow<List<UsageSummary>> = totalsSince(TimeBuckets.startOfMonth())

    /** Ukupno po danu za poslednjih 7 dana (indeks 0..6) — "Week" grafik. */
    fun observeWeeklyDailyBreakdown(): Flow<List<DailyUsage>> {
        val today = TimeBuckets.epochDay(System.currentTimeMillis())
        return hourlyDao.observeDaily(today - 6, today)
    }

    /** Ukupno po danu za tekući mesec (indeks 0 = 1. u mesecu). */
    fun observeMonthlyDailyBreakdown(): Flow<List<DailyUsage>> {
        val today = TimeBuckets.epochDay(System.currentTimeMillis())
        val firstOfMonth = TimeBuckets.epochDay(TimeBuckets.startOfMonth())
        return hourlyDao.observeDaily(firstOfMonth, today)
    }

    // --- Jedna aplikacija ---

    fun observeHourlyUsageForApp(packageName: String, dayStartTimestamp: Long): Flow<List<HourlyUsage>> =
        hourlyDao.observeHourlyForApp(TimeBuckets.epochDay(dayStartTimestamp), packageName)

    fun observeDailyUsageForApp(packageName: String): Flow<List<DailyUsage>> {
        val today = TimeBuckets.epochDay(System.currentTimeMillis())
        return hourlyDao.observeDailyForApp(today - 6, today, packageName)
    }

    fun observeSessionCountForApp(packageName: String, dayStartTimestamp: Long): Flow<Int> =
        hourlyDao.observeSessionCountForApp(TimeBuckets.epochDay(dayStartTimestamp), packageName)

    // --- Za rules engine ---

    suspend fun todayUsageByPackage(now: Long = System.currentTimeMillis()): Map<String, Long> =
        hourlyDao.totalsForDay(TimeBuckets.epochDay(now)).associate { it.packageName to it.totalDurationMs }

    // --- Sync i retencija ---

    /**
     * Inkrementalni, idempotentni sync.
     *
     * 1. Čita događaje od `safeCursor` (početak sesije koja je bila otvorena
     *    pri prošlom sync-u) — nijedna sesija ne gubi početak (B5).
     * 2. Upisuje zatvorene + trenutno otvorenu sesiju (REPLACE po paket+start).
     * 3. Preračunava satne rollup-ove od dana kursora do danas, sečene po
     *    satima u lokalnoj zoni (B6).
     *
     * Prvi sync ide [TimeBuckets.SYNC_LOOKBACK_DAYS] dana unazad, pa nedeljni
     * prikaz ima podatke odmah posle instalacije.
     */
    suspend fun syncUsageEvents(now: Long = System.currentTimeMillis()): SyncResult = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            // Bez Usage Access-a queryEvents vraća PRAZNO (bez izuzetka). Ranije je
            // takav "prazan" sync pomerao kursor na `now`, pa je sva istorija pre
            // davanja dozvole trajno preskočena → svuda 0 (bag sa telefona 27.09.).
            if (!eventSource.hasAccess()) {
                Log.w(TAG, "Sync skipped: no usage access")
                return@withLock SyncResult.NoAccess
            }

            val zone = TimeBuckets.zone()
            val today = TimeBuckets.epochDay(now, zone)
            val lookbackStart = TimeBuckets.startOfDay(today - TimeBuckets.SYNC_LOOKBACK_DAYS, zone)
            // Pun backfill istorije (ceo lookback prozor) se radi dok ne uspe jednom
            // za tekuću BACKFILL_VERSION. Ranije se oslanjao samo na "tabela je
            // prazna" — ali ako je stari proces u međuvremenu upisao par novih
            // sesija, backfill se nikad nije desio i prethodni dani su ostali prazni.
            val backfillDone = (stateDao.getLong(AppStateKeys.USAGE_BACKFILL_VERSION) ?: 0L) >= BACKFILL_VERSION &&
                usageLogDao.count() > 0
            val storedCursor = if (backfillDone) stateDao.getLong(AppStateKeys.USAGE_SYNC_CURSOR) else null
            val cursor = (storedCursor ?: lookbackStart).coerceIn(lookbackStart, now)

            val events = eventSource.read(cursor - 1, now)
            if (!backfillDone && events.isNotEmpty()) {
                // Čist početak prozora: sesije se rekonstruišu iz istih događaja,
                // pa ne ostaju delimične sesije iz ranijih (neispravnih) sync-ova.
                usageLogDao.deleteStartingFrom(cursor)
            }
            val result = reconstructor.reconstruct(events, now)
            val sessions = result.allSessions
            if (sessions.isNotEmpty()) {
                usageLogDao.upsertAll(
                    sessions.map {
                        AppUsageLog(
                            packageName = it.packageName,
                            startTimeStamp = it.start,
                            endTimeStamp = it.end,
                            durationMs = it.durationMs
                        )
                    }
                )
            }

            val firstDay = TimeBuckets.epochDay(cursor, zone)
            val raw = usageLogDao.sessionsOverlapping(TimeBuckets.startOfDay(firstDay, zone), now + 1)
            val rows = UsageSplitter
                .aggregate(raw.map { UsageSession(it.packageName, it.startTimeStamp, it.endTimeStamp) }, zone, firstDay)
                .map { (key, value) ->
                    UsageHourlyEntity(key.packageName, key.epochDay, key.hour, value.durationMs, value.sessionStarts)
                }
            hourlyDao.replaceFromDay(firstDay, rows)

            // Kursor se pomera samo kad smo zaista videli događaje; prazan prozor
            // (npr. privremeno nedostupni podaci) se sledeći put čita ponovo.
            if (events.isNotEmpty()) {
                stateDao.upsert(AppInternalState(AppStateKeys.USAGE_SYNC_CURSOR, result.safeCursor))
                if (!backfillDone) {
                    stateDao.upsert(AppInternalState(AppStateKeys.USAGE_BACKFILL_VERSION, BACKFILL_VERSION))
                }
            }
            stateDao.upsert(AppInternalState(AppStateKeys.LAST_USAGE_EVENT_SYNC_AT, now))

            Log.i(TAG, "Sync backfill=${!backfillDone} from=$cursor events=${events.size} sessions=${sessions.size} hourlyRows=${rows.size}")
            SyncResult.Synced(events = events.size, sessions = sessions.size, hourlyRows = rows.size)
        }
    }

    suspend fun lastSyncAt(): Long = stateDao.getLong(AppStateKeys.LAST_USAGE_EVENT_SYNC_AT) ?: 0L

    /** Sirove sesije: [TimeBuckets.RAW_LOG_RETENTION_DAYS]; rollup-ovi: [TimeBuckets.ROLLUP_RETENTION_DAYS] (B10). */
    suspend fun cleanupOldLogs(now: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        usageLogDao.deleteOlderThan(now - TimeBuckets.RAW_LOG_RETENTION_DAYS * TimeBuckets.DAY_MS)
        hourlyDao.deleteOlderThan(TimeBuckets.epochDay(now) - TimeBuckets.ROLLUP_RETENTION_DAYS)
    }

    private fun totalsSince(fromTimestamp: Long): Flow<List<UsageSummary>> =
        hourlyDao.observeTotals(TimeBuckets.epochDay(fromTimestamp), TimeBuckets.epochDay(System.currentTimeMillis()))

    sealed interface SyncResult {
        data object NoAccess : SyncResult
        data class Synced(val events: Int, val sessions: Int, val hourlyRows: Int) : SyncResult
    }

    private companion object {
        const val TAG = "UsageSync"

        /** Povećati kad god treba ponovo povući celu dostupnu istoriju (npr. posle ispravke sync-a). */
        const val BACKFILL_VERSION = 1L

        /** Jedan sync u isto vreme u celom procesu (servis, worker i UI dele instancu baze). */
        val syncMutex = Mutex()
    }
}
