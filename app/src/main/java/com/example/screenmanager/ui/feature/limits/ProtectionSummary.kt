package com.example.screenmanager.ui.feature.limits

import com.example.screenmanager.domain.rules.RulesSnapshot
import com.example.screenmanager.model.ShortsMode

/** Koliko pravila svake vrste je trenutno uključeno (sažetak za početni ekran i Overview). */
data class ProtectionSummary(
    val dailyLimits: Int = 0,
    val sessionLimits: Int = 0,
    val schedules: Int = 0,
    /** Mod Shorts/Reels pravila, ili null ako je isključeno. */
    val shortsMode: ShortsMode? = null,
    val morningLockEnabled: Boolean = false
) {
    /** Ukupan broj uključenih pravila svih vrsta. */
    val activeCount: Int
        get() = dailyLimits + sessionLimits + schedules +
            (if (shortsMode != null) 1 else 0) + (if (morningLockEnabled) 1 else 0)
}

fun RulesSnapshot.toProtectionSummary() = ProtectionSummary(
    dailyLimits = appLimits.count { it.isEnabled },
    sessionLimits = sessionLimits.count { it.isEnabled },
    schedules = schedules.count { it.isEnabled },
    shortsMode = shortVideo?.takeIf { it.isEnabled && it.selectedAppIds.isNotEmpty() }?.mode,
    morningLockEnabled = wakeUp?.let { it.isEnabled && it.selectedAppIds.isNotEmpty() } ?: false
)
