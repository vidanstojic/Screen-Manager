package com.example.screenmanager.service

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.ConcurrentHashMap

/**
 * Prepoznaje da li je na ekranu AKTIVAN Shorts/Reels plejer.
 *
 * Staro pravilo (`text/contentDescription` sadrži "shorts"/"reels") je
 * hvatalo i tab "Shorts"/"Reels" u donjoj navigaciji, pa se ceo YouTube
 * brojao kao Shorts (B7). Sada:
 * 1. brza nativna pretraga po poznatim view ID-jevima plejera,
 * 2. rezervni ograničen BFS po ID-jevima koji sadrže ključne reči plejera,
 *    uz isključivanje tabova/shelf-ova,
 * 3. pogodak važi samo ako je čvor vidljiv i zauzima većinu ekrana.
 *
 * Ako aplikacije promene ID-jeve, dovoljno je dopuniti liste ispod. ID nađen
 * rezervnom pretragom se pamti, pa sledeći put ide brzim putem.
 */
object ShortsDetector {
    const val YOUTUBE = "com.google.android.youtube"
    const val INSTAGRAM = "com.instagram.android"

    val supportedPackages = setOf(YOUTUBE, INSTAGRAM)

    private val playerIds = mapOf(
        YOUTUBE to listOf(
            "$YOUTUBE:id/reel_player_page_container",
            "$YOUTUBE:id/reel_recycler",
            "$YOUTUBE:id/reel_watch_player"
        ),
        INSTAGRAM to listOf(
            "$INSTAGRAM:id/clips_viewer_view_pager",
            "$INSTAGRAM:id/clips_viewer_container",
            "$INSTAGRAM:id/root_clips_layout"
        )
    )

    private val idKeywords = mapOf(
        YOUTUBE to listOf("reel_player", "reel_watch", "reel_recycler"),
        INSTAGRAM to listOf("clips_viewer", "root_clips")
    )

    private val excludedIdParts = listOf("tab", "shelf", "pivot", "nav", "button", "icon")

    private const val MAX_NODES = 600
    private const val MIN_SCREEN_FRACTION = 0.6f

    /** @property deepSearched da li je urađena skupa rezervna pretraga stabla. */
    data class Detection(val visible: Boolean, val deepSearched: Boolean)

    /** ID-jevi plejera nađeni rezervnom pretragom — sledeći put idu brzim putem. */
    private val learnedIds = ConcurrentHashMap<String, MutableSet<String>>()

    /**
     * @param allowDeepSearch rezervni BFS (do [MAX_NODES] čvorova, svaki je IPC ka aplikaciji)
     *   samo kad ga pozivalac dozvoli; brza pretraga po ID-jevima radi uvek.
     */
    fun detect(root: AccessibilityNodeInfo, packageName: String, allowDeepSearch: Boolean): Detection {
        val rootBounds = Rect().also(root::getBoundsInScreen)
        if (rootBounds.isEmpty) return Detection(visible = false, deepSearched = false)

        val known = playerIds[packageName].orEmpty() + learnedIds[packageName].orEmpty()
        known.forEach { id ->
            if (root.findAccessibilityNodeInfosByViewId(id).any { isDominant(it, rootBounds) }) {
                return Detection(visible = true, deepSearched = false)
            }
        }
        if (!allowDeepSearch) return Detection(visible = false, deepSearched = false)

        val keywords = idKeywords[packageName] ?: return Detection(visible = false, deepSearched = false)
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.addLast(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited++
            val id = node.viewIdResourceName
            if (id != null &&
                keywords.any { id.contains(it, ignoreCase = true) } &&
                excludedIdParts.none { id.contains(it, ignoreCase = true) } &&
                isDominant(node, rootBounds)
            ) {
                learnedIds.getOrPut(packageName) { ConcurrentHashMap.newKeySet() }.add(id)
                return Detection(visible = true, deepSearched = true)
            }
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(queue::addLast)
            }
        }
        return Detection(visible = false, deepSearched = true)
    }

    private fun isDominant(node: AccessibilityNodeInfo, rootBounds: Rect): Boolean {
        if (!node.isVisibleToUser) return false
        val bounds = Rect().also(node::getBoundsInScreen)
        return bounds.height() >= rootBounds.height() * MIN_SCREEN_FRACTION &&
            bounds.width() >= rootBounds.width() * MIN_SCREEN_FRACTION
    }
}
