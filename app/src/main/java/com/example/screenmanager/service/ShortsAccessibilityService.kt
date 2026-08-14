package com.example.screenmanager.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log
import com.example.screenmanager.data.repository.BlockRepository
import com.example.screenmanager.domain.ServiceLocator
import com.example.screenmanager.domain.TimeBuckets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ShortsAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val blockRepository by lazy { ServiceLocator.blockRepository(this) }
    private var sessionStartedAt = 0L
    private var lastDetectedAt = 0L
    private var lastDumpAt = 0L
    private var lastScanAt = 0L
    private val SCAN_INTERVAL_MS = 300L // Skeniraj ekran max jednom u 300ms

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return

        // 1. Proveravamo da li nas ovaj paket uopšte zanima
        if (packageName !in BlockRepository.shortsPackages) return

        // 2. Slušamo samo promene stanja i sadržaja prozora
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }

        val now = System.currentTimeMillis()

        // markInteraction ostavljamo iznad throttling-a jer je poziv asinhron (u korutini),
        // jako je brz i želimo da precizno beležimo svaku aktivnost korisnika
        serviceScope.launch { blockRepository.markInteraction(now) }

        // --- DEBOUNCE MEHANIZAM ---
        // Prekidamo izvršavanje ako je prošlo premalo vremena od prošlog skeniranja
        if (now - lastScanAt < SCAN_INTERVAL_MS) {
            return
        }
        lastScanAt = now
        // --------------------------

        // 3. Uzimamo koren UI stabla tek kada smo sigurni da želimo da skeniramo
        val root = rootInActiveWindow ?: return

        // 4. Debug dump (zadržan tvoj postojeći interval)
        if (now - lastDumpAt > DEBUG_DUMP_INTERVAL_MS) {
            lastDumpAt = now
            dumpNodeTree(root)
        }

        // 5. Pokrećemo optimizovanu proveru nad stablom
        val shortsOrReelsVisible = containsShortsOrReelsSurface(root, packageName)

        // 6. Upravljanje tajmerom i eventualno blokiranje
        handleShortsTimer(shortsOrReelsVisible, now)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun handleShortsTimer(visible: Boolean, now: Long) {
        if (!visible) {
            if (lastDetectedAt > 0L && now - lastDetectedAt > TimeBuckets.shortsGraceMs) {
                sessionStartedAt = 0L
                lastDetectedAt = 0L
            }
            return
        }

        serviceScope.launch {
            val blockedUntil = blockRepository.getShortsBlockedUntil()
            if (blockedUntil > now) {
                performGlobalAction(GLOBAL_ACTION_BACK)
                return@launch
            }

            if (sessionStartedAt == 0L) {
                sessionStartedAt = now
            }
            lastDetectedAt = now

            if (now - sessionStartedAt >= TimeBuckets.shortsLimitMs) {
                blockRepository.punishShorts(now + TimeBuckets.shortsPenaltyMs)
                sessionStartedAt = 0L
                lastDetectedAt = 0L
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        }
    }

    private fun containsShortsOrReelsSurface(
        node: AccessibilityNodeInfo?,
        packageName: String
    ): Boolean {
        if (node == null) return false

        // Izbegavamo pravljenje novih stringova! Čitamo direktno.
        val viewId = node.viewIdResourceName
        val contentDesc = node.contentDescription
        val text = node.text

        val directMatch = when (packageName) {
            "com.google.android.youtube" -> {
                (viewId != null && (viewId.contains("shorts", true) || viewId.contains("reel_watch_sequence", true) || viewId.contains("shorts_shelf", true))) ||
                        (contentDesc != null && (contentDesc.contains("shorts", true) || contentDesc.contains("reel_watch_sequence", true))) ||
                        (text != null && text.contains("shorts", true))
            }
            "com.instagram.android" -> {
                (viewId != null && (viewId.contains("reels", true) || viewId.contains("clips", true) || viewId.contains("reel", true))) ||
                        (contentDesc != null && (contentDesc.contains("reels", true) || contentDesc.contains("clips", true) || contentDesc.contains("reel", true)))
            }
            else -> false
        }

        if (directMatch) return true

        // Rekurzivni prolazak
        for (index in 0 until node.childCount) {
            if (containsShortsOrReelsSurface(node.getChild(index), packageName)) {
                return true
            }
        }
        return false
    }

    private fun dumpNodeTree(root: AccessibilityNodeInfo) {
        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            val indent = "  ".repeat(depth.coerceAtMost(12))
            Log.d(
                TAG,
                "$indent id=${node.viewIdResourceName} class=${node.className} text=${node.text} desc=${node.contentDescription}"
            )
            for (index in 0 until node.childCount) {
                val child = node.getChild(index) ?: continue
                visit(child, depth + 1)
            }
        }
        visit(root, 0)
    }

    companion object {
        private const val TAG = "ShortsAccessibility"
        private const val DEBUG_DUMP_INTERVAL_MS = 2_000L
    }
}
