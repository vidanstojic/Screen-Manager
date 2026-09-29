package com.example.screenmanager.service

import android.accessibilityservice.AccessibilityService
import android.content.pm.ApplicationInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
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
 * - BUDGET: dnevni budžet po aplikaciji → kazna za celu aplikaciju (sprovodi
 *   je FocusMonitorService preko RulesEngine-a) i BACK do kraja dana.
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
    private val storedWatchMs = HashMap<String, Long>()
    private var lastDumpAt = 0L
    private var lastKickToastAt = 0L
    private val sessionCache = HashMap<String, SessionState?>()
    private val sessionPersistedAt = HashMap<String, Long>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        shortsConfig = settingsRepository.observeShortVideoConfig()
            .stateIn(serviceScope, SharingStarted.Eagerly, null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (packageName !in ShortsDetector.supportedPackages) return

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
        val config = shortsConfig.value ?: return
        val root = rootInActiveWindow
        val packageName = root?.packageName?.toString() ?: lastEventPackage
        val now = System.currentTimeMillis()

        if (root == null || packageName == null || !config.isEnabled || packageName !in config.selectedAppIds) {
            onShortsHidden(now)
            return
        }

        maybeDumpTree(root, now)

        if (ShortsDetector.isShortFormVisible(root, packageName)) {
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
        val key = ShortsPolicy.stateKey(packageName)
        val previous = if (sessionCache.containsKey(key)) sessionCache[key] else runtimeRepository.sessionState(key)
        val (next, verdict) = ShortsPolicy.onVisible(
            config = config,
            packageName = packageName,
            previous = previous,
            now = now,
            zone = TimeBuckets.zone(),
            startOfNextDay = TimeBuckets.startOfNextDay(now)
        )

        if (next != null && next != previous) {
            sessionCache[key] = next
            val structural = previous == null ||
                previous.sessionsUsed != next.sessionsUsed ||
                previous.sessionStartedAt != next.sessionStartedAt ||
                previous.frozenUntil != next.frozenUntil ||
                previous.epochDay != next.epochDay
            if (structural || now - (sessionPersistedAt[key] ?: 0L) >= FLUSH_INTERVAL_MS) {
                runtimeRepository.saveSessionStates(listOf(next))
                sessionPersistedAt[key] = now
            }
        }

        when (verdict) {
            ShortsVerdict.Allow -> scheduleFollowUp()
            is ShortsVerdict.Kick -> {
                followUpJob?.cancel()
                performGlobalAction(GLOBAL_ACTION_BACK)
                notifyKick(verdict, now)
            }
        }
    }

    /** Kratko objašnjenje zašto je korisnik izbačen (najviše jednom u par sekundi). */
    private suspend fun notifyKick(verdict: ShortsVerdict.Kick, now: Long) {
        if (now - lastKickToastAt < KICK_TOAST_INTERVAL_MS) return
        lastKickToastAt = now
        val remaining = verdict.until?.let { (it - now).coerceAtLeast(0) }
        val suffix = when {
            remaining == null -> ""
            remaining >= TimeBuckets.HOUR_MS -> " · još ${remaining / TimeBuckets.HOUR_MS}h ${(remaining % TimeBuckets.HOUR_MS) / TimeBuckets.MINUTE_MS}min"
            else -> " · još ${remaining / TimeBuckets.MINUTE_MS}:${"%02d".format((remaining / 1000) % 60)}"
        }
        withContext(Dispatchers.Main) {
            Toast.makeText(this@ShortsAccessibilityService, verdict.reason + suffix, Toast.LENGTH_SHORT).show()
        }
    }

    /** BUDGET mod: dnevni budžet gledanja + kazna za celu aplikaciju. */
    private suspend fun onBudgetVisible(packageName: String, config: ShortVideoConfig, now: Long) {
        val day = TimeBuckets.epochDay(now)
        if (day != cachedDay) {
            storedWatchMs.clear()
            pendingWatchMs = 0L
            cachedDay = day
        }

        if (visiblePackage != null && visiblePackage != packageName) flush(now)
        if (visiblePackage == packageName && now - lastVisibleAt <= VISIBILITY_GRACE_MS) {
            pendingWatchMs += now - lastVisibleAt
        }
        visiblePackage = packageName
        lastVisibleAt = now

        val stored = storedWatchMs.getOrPut(packageName) { runtimeRepository.shortsWatchedMs(packageName, day) }
        val limitMs = config.maxReelsWatchMinutes.coerceAtLeast(0) * TimeBuckets.MINUTE_MS
        val total = stored + pendingWatchMs

        if (total >= limitMs) {
            if (stored < limitMs) {
                // Upravo prešli budžet → kazna za celu aplikaciju.
                flush(now)
                runtimeRepository.setShortsPenalty(
                    packageName,
                    now + config.fullAppBlockMinutes.coerceAtLeast(1) * TimeBuckets.MINUTE_MS
                )
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
        val packageName = visiblePackage ?: return
        lastFlushAt = now
        if (pendingWatchMs <= 0L) return
        val total = runtimeRepository.addShortsWatchedMs(packageName, cachedDay, pendingWatchMs)
        storedWatchMs[packageName] = total
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
            node.viewIdResourceName?.let { Log.d(TAG, "${"  ".repeat(depth.coerceAtMost(12))}id=$it class=${node.className}") }
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
        private const val DEBUG_DUMP_INTERVAL_MS = 10_000L
        private const val KICK_TOAST_INTERVAL_MS = 4_000L
        private const val DEBUG_DUMP_MAX_NODES = 400
    }
}
