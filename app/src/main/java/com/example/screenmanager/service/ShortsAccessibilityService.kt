package com.example.screenmanager.service

import android.accessibilityservice.AccessibilityService
import android.content.pm.ApplicationInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.example.screenmanager.alarm.AlarmModule
import com.example.screenmanager.alarm.service.WebsiteGuard
import com.example.screenmanager.domain.ServiceLocator
import com.example.screenmanager.domain.TimeBuckets
import com.example.screenmanager.domain.rules.SessionState
import com.example.screenmanager.domain.rules.ShortsPolicy
import com.example.screenmanager.domain.rules.ShortsVerdict
import com.example.screenmanager.model.ShortVideoConfig
import com.example.screenmanager.model.ShortsMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Shorts/Reels blocker (TRS 2.3).
 *
 * Performanse (B8): `onAccessibilityEvent` (main thread) samo zakazuje skeniranje;
 * obilazak stabla radi na jednom serijskom pozadinskom dispatcher-u, sa
 * debounce-om, a manifest config već filtrira događaje na 2 paketa.
 *
 * Tačnost (B7): detekcija preko [ShortsDetector] (ID plejera + veličina),
 * ne preko teksta "Shorts" koji postoji i u navigacionom tabu.
 *
 * Pravila dolaze iz korisnikovog [ShortVideoConfig], po [ShortsMode]:
 * - BLOCKED: svaki ulazak u Shorts/Reels → BACK;
 * - SESSIONS: interval mod (M/N/K) samo za Shorts/Reels → BACK tokom pauze
 *   i posle N-te sesije do ponoći ([ShortsPolicy] + SessionLimitTracker);
 * - BUDGET: zajednički dnevni budžet → kazna za sve izabrane aplikacije
 *   (sprovodi je FocusMonitorService preko RulesEngine-a) i BACK do kraja dana.
 * YouTube Shorts i Instagram Reels dele isti brojač (sesije i budžet).
 * Ostatak aplikacije (obični video, poruke, feed) radi normalno u svim modovima.
 */
class ShortsAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val scanDispatcher = Dispatchers.Default.limitedParallelism(1)

    private val runtimeRepository by lazy { ServiceLocator.runtimeStateRepository(this) }
    private val settingsRepository by lazy { ServiceLocator.settingsRepository(this) }
    private val isDebuggable by lazy { applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0 }

    @Volatile
    private var shortsConfig: StateFlow<ShortVideoConfig?> = MutableStateFlow(null)

    private var websiteGuard: WebsiteGuard? = null

    private val scanScheduled = AtomicBoolean(false)
    @Volatile private var lastEventPackage: String? = null
    @Volatile private var lastInteractionWriteAt = 0L

    // Sledeće promenljive menja isključivo scanDispatcher (serijski).
    private var followUpJob: Job? = null
    private var visiblePackage: String? = null
    private var lastVisibleAt = 0L
    private var pendingWatchMs = 0L
    private var lastFlushAt = 0L
    private var cachedDay = -1L
    /** Danas odgledano (zbir svih short-form aplikacija), već upisano u bazu; null = nije učitano. */
    private var storedWatchMs: Long? = null
    private var lastDumpAt = 0L
    private var lastKickToastAt = 0L
    private var sessionLoaded = false
    private var sessionState: SessionState? = null
    private var sessionPersistedAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Service connected")
        shortsConfig = settingsRepository.observeShortVideoConfig()
            .stateIn(serviceScope, SharingStarted.Eagerly, null)
        // Smart Alarm wake-up detox: blokada sajtova radi u ovom istom servisu.
        websiteGuard = WebsiteGuard(
            service = this,
            scope = serviceScope,
            scanDispatcher = scanDispatcher,
            detox = AlarmModule.detoxStore(this).lock,
            basePackages = ShortsDetector.supportedPackages
        ).also { it.start() }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (packageName !in ShortsDetector.supportedPackages) {
            websiteGuard?.takeIf { it.handles(packageName) }?.onEvent(packageName)
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastInteractionWriteAt > INTERACTION_WRITE_INTERVAL_MS) {
            lastInteractionWriteAt = now
            serviceScope.launch { runtimeRepository.markInteraction(now) }
        }

        lastEventPackage = packageName
        scheduleScan(SCAN_DEBOUNCE_MS)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        Log.i(TAG, "Service destroyed")
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun scheduleScan(delayMs: Long) {
        if (!scanScheduled.compareAndSet(false, true)) return
        serviceScope.launch(scanDispatcher) {
            delay(delayMs)
            scanScheduled.set(false)
            runCatching { scan() }.onFailure { Log.w(TAG, "Shorts scan failed", it) }
        }
    }

    private suspend fun scan() {
        val config = shortsConfig.value
        if (config == null) {
            Log.d(TAG, "Scan skipped: config not loaded yet")
            return
        }
        val root = rootInActiveWindow
        val packageName = root?.packageName?.toString() ?: lastEventPackage
        val now = System.currentTimeMillis()

        if (root == null || packageName == null || !config.isEnabled || packageName !in config.selectedAppIds) {
            Log.d(TAG, "Scan skipped: root=${root != null} pkg=$packageName enabled=${config.isEnabled} apps=${config.selectedAppIds}")
            onShortsHidden(now)
            return
        }

        maybeDumpTree(root, now)

        val visible = ShortsDetector.isShortFormVisible(root, packageName)
        if (isDebuggable) Log.d(TAG, "Scan pkg=$packageName mode=${config.mode} shortFormVisible=$visible")
        if (visible) {
            onShortsVisible(packageName, config, now)
        } else {
            onShortsHidden(now)
        }
    }

    private suspend fun onShortsVisible(packageName: String, config: ShortVideoConfig, now: Long) {
        when (config.mode) {
            ShortsMode.BUDGET -> onBudgetVisible(packageName, config, now)
            ShortsMode.BLOCKED, ShortsMode.SESSIONS -> onPolicyVisible(packageName, config, now)
        }
    }

    /**
     * BLOCKED i SESSIONS modovi: odluku donosi čist [ShortsPolicy]; ovde se
     * samo čuva stanje interval sesije i izvršava BACK + kratko obaveštenje.
     */
    private suspend fun onPolicyVisible(packageName: String, config: ShortVideoConfig, now: Long) {
        val previous = loadSessionState()
        val (next, verdict) = ShortsPolicy.onVisible(
            config = config,
            previous = previous,
            now = now,
            zone = TimeBuckets.zone(),
            startOfNextDay = TimeBuckets.startOfNextDay(now)
        )

        saveSessionState(previous, next, now)

        when (verdict) {
            ShortsVerdict.Allow -> scheduleFollowUp()
            is ShortsVerdict.Kick -> {
                followUpJob?.cancel()
                performGlobalAction(GLOBAL_ACTION_BACK)
                notifyKick(verdict, now)
            }
        }
    }

    private suspend fun loadSessionState(): SessionState? {
        if (!sessionLoaded) {
            sessionState = runtimeRepository.sessionState(ShortsPolicy.SHARED_STATE_KEY)
            sessionLoaded = true
        }
        return sessionState
    }

    /** Strukturne promene (nova sesija, pauza, novi dan) odmah u bazu, ostalo najviše na [FLUSH_INTERVAL_MS]. */
    private suspend fun saveSessionState(previous: SessionState?, next: SessionState?, now: Long) {
        if (next == null || next == previous) return
        sessionState = next
        val structural = previous == null ||
            previous.sessionsUsed != next.sessionsUsed ||
            previous.sessionStartedAt != next.sessionStartedAt ||
            previous.frozenUntil != next.frozenUntil ||
            previous.epochDay != next.epochDay
        if (structural || now - sessionPersistedAt >= FLUSH_INTERVAL_MS) {
            runtimeRepository.saveSessionStates(listOf(next))
            sessionPersistedAt = now
        }
    }

    /** Kratko objašnjenje zašto je korisnik izbačen (najviše jednom u par sekundi). */
    private suspend fun notifyKick(verdict: ShortsVerdict.Kick, now: Long) {
        if (now - lastKickToastAt < KICK_TOAST_INTERVAL_MS) return
        lastKickToastAt = now
        val remaining = verdict.until?.let { (it - now).coerceAtLeast(0) }
        val suffix = when {
            remaining == null -> ""
            remaining >= TimeBuckets.HOUR_MS -> " · ${remaining / TimeBuckets.HOUR_MS}h ${(remaining % TimeBuckets.HOUR_MS) / TimeBuckets.MINUTE_MS}m left"
            else -> " · ${remaining / TimeBuckets.MINUTE_MS}:${"%02d".format((remaining / 1000) % 60)} left"
        }
        withContext(Dispatchers.Main) {
            Toast.makeText(this@ShortsAccessibilityService, verdict.reason + suffix, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * BUDGET mod: zajednički dnevni budžet gledanja za sve izabrane aplikacije.
     * Kad se potroši, sve one dobijaju kaznu (cela aplikacija), a Shorts/Reels
     * ostaju zatvoreni do ponoći u svakoj od njih.
     */
    private suspend fun onBudgetVisible(packageName: String, config: ShortVideoConfig, now: Long) {
        val day = TimeBuckets.epochDay(now)
        if (day != cachedDay) {
            storedWatchMs = null
            pendingWatchMs = 0L
            cachedDay = day
        }

        // Prelazak iz jedne aplikacije u drugu ne prekida brojanje: budžet je zajednički.
        if (visiblePackage != null && now - lastVisibleAt <= VISIBILITY_GRACE_MS) {
            pendingWatchMs += now - lastVisibleAt
        }
        visiblePackage = packageName
        lastVisibleAt = now

        val stored = storedWatchMs ?: runtimeRepository.shortsWatchedMs(ShortsPolicy.SHARED_WATCH_KEY, day)
            .also { storedWatchMs = it }

        if (ShortsPolicy.budgetExhausted(config, stored + pendingWatchMs)) {
            if (!ShortsPolicy.budgetExhausted(config, stored)) {
                // Upravo prešli budžet → kazna za sve izabrane aplikacije.
                flush(now)
                val until = now + config.fullAppBlockMinutes.coerceAtLeast(1) * TimeBuckets.MINUTE_MS
                config.selectedAppIds.forEach { runtimeRepository.setShortsPenalty(it, until) }
            }
            performGlobalAction(GLOBAL_ACTION_BACK)
            visiblePackage = null
            return
        }

        if (now - lastFlushAt >= FLUSH_INTERVAL_MS) flush(now)
        scheduleFollowUp()
    }

    private suspend fun onShortsHidden(now: Long) {
        if (visiblePackage != null) flush(now)
        visiblePackage = null
        followUpJob?.cancel()
    }

    /**
     * Pasivno gledanje jednog Short-a ne mora da generiše događaje, pa dok je
     * plejer vidljiv ponovo skeniramo na [FOLLOW_UP_SCAN_MS].
     */
    private fun scheduleFollowUp() {
        if (followUpJob?.isActive == true) return
        followUpJob = serviceScope.launch(scanDispatcher) {
            delay(FOLLOW_UP_SCAN_MS)
            followUpJob = null // da bi scan() mogao da zakaže sledeći follow-up
            runCatching { scan() }.onFailure { Log.w(TAG, "Shorts follow-up scan failed", it) }
        }
    }

    private suspend fun flush(now: Long) {
        if (visiblePackage == null) return
        lastFlushAt = now
        if (pendingWatchMs <= 0L) return
        storedWatchMs = runtimeRepository.addShortsWatchedMs(ShortsPolicy.SHARED_WATCH_KEY, cachedDay, pendingWatchMs)
        pendingWatchMs = 0L
    }

    /** Samo u debug build-u: pomaže da se kalibrišu ID-jevi u [ShortsDetector]. */
    private fun maybeDumpTree(root: AccessibilityNodeInfo, now: Long) {
        if (!isDebuggable || now - lastDumpAt < DEBUG_DUMP_INTERVAL_MS) return
        lastDumpAt = now
        val queue = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        queue.addLast(root to 0)
        var visited = 0
        while (queue.isNotEmpty() && visited < DEBUG_DUMP_MAX_NODES) {
            val (node, depth) = queue.removeFirst()
            visited++
            if (node.viewIdResourceName != null || node.contentDescription != null) {
                val bounds = android.graphics.Rect().also(node::getBoundsInScreen)
                Log.d(
                    TAG,
                    "DUMP ${"  ".repeat(depth.coerceAtMost(12))}id=${node.viewIdResourceName} " +
                        "desc=${node.contentDescription} class=${node.className} vis=${node.isVisibleToUser} " +
                        "sel=${node.isSelected} bounds=$bounds"
                )
            }
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let { queue.addLast(it to depth + 1) }
            }
        }
    }

    companion object {
        private const val TAG = "ShortsAccessibility"
        private const val SCAN_DEBOUNCE_MS = 250L
        private const val FOLLOW_UP_SCAN_MS = 2_000L
        private const val VISIBILITY_GRACE_MS = 4_000L
        private const val FLUSH_INTERVAL_MS = 5_000L
        private const val INTERACTION_WRITE_INTERVAL_MS = 5_000L
        private const val DEBUG_DUMP_INTERVAL_MS = 5_000L
        private const val KICK_TOAST_INTERVAL_MS = 4_000L
        private const val DEBUG_DUMP_MAX_NODES = 400
    }
}
