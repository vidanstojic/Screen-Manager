package com.example.screenmanager.service

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.example.screenmanager.MainActivity
import com.example.screenmanager.R
import com.example.screenmanager.data.usage.UsageEventSource
import com.example.screenmanager.domain.ServiceLocator
import com.example.screenmanager.domain.TimeBuckets
import com.example.screenmanager.domain.rules.RulesSnapshot
import com.example.screenmanager.domain.rules.RuntimeState
import com.example.screenmanager.domain.rules.SessionLimitTracker
import com.example.screenmanager.domain.rules.SessionState
import com.example.screenmanager.domain.usage.ForegroundResolver
import com.example.screenmanager.domain.wakeup.WakeUpSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.ZoneId

/**
 * Foreground servis koji sprovodi SVA pravila (TRS 2.2, 2.4, 2.5) i drži
 * statistiku svežom.
 *
 * Petlje (samo dok je ekran upaljen — kad je ugašen, korutine su
 * suspendovane na [interactive], pa nema CPU rada):
 * - monitor (2s): inkrementalno čita UsageEvents → foreground paket
 *   ([ForegroundResolver], B3) → interval sesije → [RulesEngine] → overlay.
 * - sync (60s): [com.example.screenmanager.domain.UsageStatsRepository.syncUsageEvents]
 *   i keš današnje potrošnje po paketu za dnevne limite.
 *
 * Pokreće se iz MainActivity i BootReceiver-a preko [start] (B2).
 */
class FocusMonitorService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val usageRepository by lazy { ServiceLocator.usageStatsRepository(this) }
    private val runtimeRepository by lazy { ServiceLocator.runtimeStateRepository(this) }
    private val settingsRepository by lazy { ServiceLocator.settingsRepository(this) }
    private val wakeUpTrigger by lazy { ServiceLocator.wakeUpLockoutTrigger(this) }
    private val rulesEngine = ServiceLocator.rulesEngine()
    private val eventSource by lazy { UsageEventSource(this) }
    private val powerManager by lazy { getSystemService(PowerManager::class.java) }

    private lateinit var overlayController: BlockOverlayController
    private val interactive = MutableStateFlow(true)

    private lateinit var rules: StateFlow<RulesSnapshot>
    private lateinit var wakeUpBlockedUntil: StateFlow<Long?>
    private lateinit var shortsPenalties: StateFlow<Map<String, Long>>

    // Stanje monitor petlje — menja se samo iz monitor korutine.
    private val foreground = ForegroundResolver()
    private var eventCursor = 0L
    private val sessionStates = HashMap<String, SessionState>()
    private var sessionStatesPersistedAt = 0L
    private val sessionMutex = Mutex()

    // Deljeno između sync i monitor korutina.
    @Volatile private var todayUsage: Map<String, Long> = emptyMap()
    @Volatile private var lastSyncAt = 0L

    private var loopsStarted = false
    private var wakeUpEvaluationJob: Job? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> onScreenOff()
                Intent.ACTION_SCREEN_ON -> onScreenOn()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        overlayController = BlockOverlayController(this) { sendHome() }
        interactive.value = powerManager.isInteractive
        registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            },
            RECEIVER_NOT_EXPORTED
        )
        ServiceLocator.scheduleDailyMaintenance(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } catch (error: ForegroundServiceStartNotAllowedException) {
            Log.w(TAG, "Foreground start not allowed right now", error)
            stopSelf()
            return START_NOT_STICKY
        }
        startLoops()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenReceiver) }
        overlayController.hide()
        val snapshot = sessionStates.values.toList()
        serviceScope.launch(NonCancellable) { runtimeRepository.saveSessionStates(snapshot) }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startLoops() {
        if (loopsStarted) return
        loopsStarted = true

        rules = settingsRepository.observeRulesSnapshot()
            .stateIn(serviceScope, SharingStarted.Eagerly, RulesSnapshot.EMPTY)
        wakeUpBlockedUntil = runtimeRepository.observeWakeUpBlockedUntil()
            .stateIn(serviceScope, SharingStarted.Eagerly, null)
        shortsPenalties = runtimeRepository.observeShortsPenalties()
            .stateIn(serviceScope, SharingStarted.Eagerly, emptyMap())

        serviceScope.launch {
            sessionMutex.withLock { sessionStates.putAll(runtimeRepository.sessionStates()) }
            while (isActive) {
                interactive.first { it }
                runCatching { tick() }.onFailure { Log.w(TAG, "Monitor tick failed", it) }
                delay(TICK_INTERVAL_MS)
            }
        }

        serviceScope.launch {
            while (isActive) {
                interactive.first { it }
                syncUsage()
                delay(SYNC_INTERVAL_MS)
            }
        }
    }

    private suspend fun syncUsage() {
        runCatching {
            usageRepository.syncUsageEvents()
            todayUsage = usageRepository.todayUsageByPackage()
            lastSyncAt = System.currentTimeMillis()
        }.onFailure { Log.w(TAG, "Usage sync failed", it) }
    }

    private suspend fun tick() {
        val now = System.currentTimeMillis()
        val zone = TimeBuckets.zone()
        val foregroundPackage = pollForeground(now)
        val snapshot = rules.value
        val emergencyActive = snapshot.emergency?.isActiveAt(now) == true

        if (!emergencyActive) advanceSessions(snapshot, foregroundPackage, now, zone)

        val decision = foregroundPackage
            ?.takeIf { it != packageName }
            ?.let { pkg ->
                val state = RuntimeState(
                    todayUsageMs = liveUsage(pkg, now),
                    wakeUpBlockedUntil = wakeUpBlockedUntil.value ?: 0L,
                    shortsPenaltyUntil = shortsPenalties.value,
                    sessionStates = sessionMutex.withLock { HashMap(sessionStates) }
                )
                rulesEngine.evaluate(pkg, now, zone, snapshot, state)
            }

        withContext(Dispatchers.Main) {
            if (decision != null) overlayController.show(decision) else overlayController.hide()
        }
    }

    /**
     * Inkrementalno čitanje događaja od poslednjeg obrađenog trenutka.
     * Stanje foreground-a se čuva između poziva, pa dugo sedenje u jednoj
     * aplikaciji bez novih događaja više ne "otključava" blokadu (B3).
     */
    private fun pollForeground(now: Long): String? {
        val from = if (eventCursor == 0L) now - INITIAL_EVENT_LOOKBACK_MS else eventCursor
        val events = eventSource.read(from, now + 1)
        events.forEach(foreground::apply)
        eventCursor = maxOf(from, (events.lastOrNull()?.timestamp ?: (from - 1)) + 1)
        return foreground.currentPackage
    }

    /** Današnja potrošnja + deo tekuće sesije koji poslednji sync još nije video. */
    private fun liveUsage(pkg: String, now: Long): Map<String, Long> {
        val base = todayUsage
        val since = maxOf(lastSyncAt, foreground.since, TimeBuckets.startOfToday(now))
        if (foreground.currentPackage != pkg || since <= 0L || since >= now) return base
        return base + (pkg to (base[pkg] ?: 0L) + (now - since))
    }

    private suspend fun advanceSessions(snapshot: RulesSnapshot, pkg: String?, now: Long, zone: ZoneId) {
        val rulesById = snapshot.sessionLimits.filter { it.isEnabled }
        if (rulesById.isEmpty()) return
        var structuralChange = false
        var anyChange = false
        sessionMutex.withLock {
            for (rule in rulesById) {
                val previous = sessionStates[rule.id]
                val next = SessionLimitTracker.advance(rule, previous, pkg != null && pkg in rule.selectedAppIds, now, zone)
                if (next != previous) {
                    anyChange = true
                    if (previous == null ||
                        previous.sessionsUsed != next.sessionsUsed ||
                        previous.sessionStartedAt != next.sessionStartedAt ||
                        previous.frozenUntil != next.frozenUntil ||
                        previous.epochDay != next.epochDay
                    ) {
                        structuralChange = true
                    }
                    sessionStates[rule.id] = next
                }
            }
        }
        if (structuralChange || (anyChange && now - sessionStatesPersistedAt > SESSION_PERSIST_INTERVAL_MS)) {
            persistSessionStates(now)
        }
    }

    private suspend fun persistSessionStates(now: Long) {
        val snapshot = sessionMutex.withLock { sessionStates.values.toList() }
        runtimeRepository.saveSessionStates(snapshot)
        sessionStatesPersistedAt = now
    }

    private fun onScreenOff() {
        interactive.value = false
        wakeUpEvaluationJob?.cancel()
        overlayController.hide()
        val now = System.currentTimeMillis()
        serviceScope.launch {
            runtimeRepository.markScreenOff(now)
            persistSessionStates(now)
            syncUsage()
        }
    }

    private fun onScreenOn() {
        val now = System.currentTimeMillis()
        interactive.value = true
        serviceScope.launch {
            val lastOff = runtimeRepository.lastScreenOff()
            runtimeRepository.markScreenOn(now)
            val config = settingsRepository.getWakeUpConfig() ?: return@launch
            if (!config.isEnabled || lastOff == null) return@launch
            if (now - lastOff >= config.inactivityHours * TimeBuckets.HOUR_MS) {
                startWakeUpEvaluation(now, config.triggerDelayMinutes)
            }
        }
    }

    /**
     * Posle dugog mraka ekran se upalio: ako je korisnik i dalje aktivan
     * posle `triggerDelayMinutes`, pali se jutarnja blokada (TRS 2.5).
     */
    private fun startWakeUpEvaluation(startedAt: Long, triggerDelayMinutes: Int) {
        wakeUpEvaluationJob?.cancel()
        wakeUpEvaluationJob = serviceScope.launch {
            delay(triggerDelayMinutes.coerceAtLeast(0) * TimeBuckets.MINUTE_MS)
            val lastInteraction = runtimeRepository.lastInteraction() ?: 0L
            val userStayedActive = interactive.value &&
                (foreground.currentPackage != null || lastInteraction >= startedAt)
            if (userStayedActive) {
                wakeUpTrigger.trigger(WakeUpSource.SCREEN_HEURISTIC)
            }
        }
    }

    private fun sendHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Focus monitoring", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Praćenje fokusa i ekranskog vremena je aktivno")
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "focus_monitor"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "FocusMonitorService"
        private const val TICK_INTERVAL_MS = 2_000L
        private const val SYNC_INTERVAL_MS = 60_000L
        private const val SESSION_PERSIST_INTERVAL_MS = 15_000L
        private const val INITIAL_EVENT_LOOKBACK_MS = 10 * 60_000L

        /**
         * Pokreće servis ako je data Usage Access dozvola (bez nje servis
         * nema šta da radi). Bezbedno za višestruko pozivanje.
         */
        fun start(context: Context) {
            if (!ServiceLocator.permissionStateChecker(context).snapshot().hasUsageAccess) return
            try {
                context.startForegroundService(Intent(context, FocusMonitorService::class.java))
            } catch (error: IllegalStateException) {
                // ForegroundServiceStartNotAllowedException nasleđuje IllegalStateException.
                Log.w(TAG, "Unable to start FocusMonitorService", error)
            }
        }
    }
}
