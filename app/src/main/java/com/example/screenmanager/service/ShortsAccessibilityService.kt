package com.example.screenmanager.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log
import com.example.screenmanager.domain.BlockRepository
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (packageName !in BlockRepository.shortsPackages) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }

        val now = System.currentTimeMillis()
        serviceScope.launch { blockRepository.markInteraction(now) }
        val root = rootInActiveWindow ?: return

        if (now - lastDumpAt > DEBUG_DUMP_INTERVAL_MS) {
            lastDumpAt = now
            dumpNodeTree(root)
        }

        val shortsOrReelsVisible = containsShortsOrReelsSurface(root, packageName)
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
        node: AccessibilityNodeInfo,
        packageName: String
    ): Boolean {
        val text = node.text?.toString().orEmpty()
        val contentDescription = node.contentDescription?.toString().orEmpty()
        val viewId = node.viewIdResourceName.orEmpty()
        val className = node.className?.toString().orEmpty()
        val haystack = "$text $contentDescription $viewId $className".lowercase()

        val directMatch = when (packageName) {
            "com.google.android.youtube" -> "shorts" in haystack ||
                "reel_watch_sequence" in haystack ||
                "shorts_shelf" in haystack

            "com.instagram.android" -> "reels" in haystack ||
                "clips" in haystack ||
                "reel" in haystack

            else -> false
        }
        if (directMatch) return true

        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            if (containsShortsOrReelsSurface(child, packageName)) return true
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
