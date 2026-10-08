package com.example.screenmanager.alarm.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.example.screenmanager.alarm.domain.AlarmDetoxLock
import com.example.screenmanager.alarm.domain.SiteRules
import com.example.screenmanager.ui.common.formatClock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Blokada sajtova tokom wake-up detox-a, unutar postojećeg Accessibility
 * servisa (korisnik ne mora da uključuje još jedan).
 *
 * - Dok detox sa sajtovima NE traje, servis ne prima događaje iz browsera:
 *   lista paketa u [AccessibilityService.getServiceInfo] sadrži samo
 *   [basePackages] (YouTube/Instagram za Shorts). Browseri se dodaju tek
 *   kad detox počne i uklanjaju kad istekne → nula troška ostatak dana.
 * - Adresa se čita iz adresne trake poznatih browsera (view ID). Polje u
 *   koje korisnik upravo kuca se preskače (tamo je pretraga, ne adresa).
 * - Blokiran sajt → Home + kratka poruka do kada traje detox.
 */
class WebsiteGuard(
    private val service: AccessibilityService,
    private val scope: CoroutineScope,
    private val scanDispatcher: CoroutineDispatcher,
    private val detox: StateFlow<AlarmDetoxLock?>,
    private val basePackages: Set<String>
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scanScheduled = AtomicBoolean(false)
    private var expiryJob: Job? = null
    @Volatile private var lastPackage: String? = null
    @Volatile private var lastBlockAt = 0L

    fun start() {
        scope.launch {
            detox.collect { lock ->
                val now = System.currentTimeMillis()
                val watching = lock != null && lock.isActiveAt(now) && lock.domains.isNotEmpty()
                updateWatchedPackages(watching)
                expiryJob?.cancel()
                if (watching) {
                    expiryJob = scope.launch {
                        delay(lock!!.until - now)
                        updateWatchedPackages(false)
                    }
                }
            }
        }
    }

    /** Da li je [packageName] browser koji ovaj čuvar obrađuje. */
    fun handles(packageName: String): Boolean = packageName in URL_BAR_IDS

    fun onEvent(packageName: String) {
        val lock = detox.value ?: return
        if (!lock.isActiveAt(System.currentTimeMillis()) || lock.domains.isEmpty()) return
        lastPackage = packageName
        if (!scanScheduled.compareAndSet(false, true)) return
        scope.launch(scanDispatcher) {
            delay(SCAN_DEBOUNCE_MS)
            scanScheduled.set(false)
            runCatching { scan() }.onFailure { Log.w(TAG, "Website scan failed", it) }
        }
    }

    private fun scan() {
        val lock = detox.value ?: return
        val now = System.currentTimeMillis()
        if (!lock.isActiveAt(now)) return
        val root = service.rootInActiveWindow ?: return
        val packageName = root.packageName?.toString() ?: lastPackage ?: return
        val ids = URL_BAR_IDS[packageName] ?: return
        val host = ids.asSequence()
            .flatMap { id -> root.findAccessibilityNodeInfosByViewId("$packageName:id/$id").asSequence() }
            .filterNot(AccessibilityNodeInfo::isFocused)
            .mapNotNull { node -> node.text?.toString()?.let(SiteRules::hostOf) }
            .firstOrNull() ?: return
        if (!SiteRules.isBlocked(host, lock.domains)) return
        if (now - lastBlockAt < BLOCK_COOLDOWN_MS) return
        lastBlockAt = now
        Log.i(TAG, "Blocked $host in $packageName (wake-up detox)")
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        mainHandler.post {
            Toast.makeText(
                service,
                "$host is blocked until ${formatClock(lock.until)} · wake-up detox",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun updateWatchedPackages(includeBrowsers: Boolean) {
        mainHandler.post {
            val info = service.serviceInfo ?: return@post
            val packages = if (includeBrowsers) basePackages + URL_BAR_IDS.keys else basePackages
            info.packageNames = packages.toTypedArray()
            service.serviceInfo = info
            Log.i(TAG, "Watching browsers: $includeBrowsers")
        }
    }

    companion object {
        private const val TAG = "WebsiteGuard"
        private const val SCAN_DEBOUNCE_MS = 300L
        private const val BLOCK_COOLDOWN_MS = 1_500L

        /** Browser → ID-jevi adresne trake (bez "paket:id/"). */
        val URL_BAR_IDS: Map<String, List<String>> = mapOf(
            "com.android.chrome" to listOf("url_bar"),
            "com.chrome.beta" to listOf("url_bar"),
            "com.chrome.dev" to listOf("url_bar"),
            "com.sec.android.app.sbrowser" to listOf("location_bar_edit_text", "custom_tab_toolbar_url_bar_text"),
            "com.sec.android.app.sbrowser.beta" to listOf("location_bar_edit_text"),
            "org.mozilla.firefox" to listOf("mozac_browser_toolbar_url_view", "url_bar_title"),
            "com.microsoft.emmx" to listOf("url_bar"),
            "com.brave.browser" to listOf("url_bar"),
            "com.opera.browser" to listOf("url_field"),
            "com.vivaldi.browser" to listOf("url_bar"),
            "com.kiwibrowser.browser" to listOf("url_bar"),
            "com.duckduckgo.mobile.android" to listOf("omnibarTextInput")
        )
    }
}
