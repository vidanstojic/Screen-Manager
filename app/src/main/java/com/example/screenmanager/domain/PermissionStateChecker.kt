package com.example.screenmanager.domain

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import com.example.screenmanager.service.ShortsAccessibilityService

data class PermissionState(
    val hasUsageAccess: Boolean,
    val canDrawOverlay: Boolean,
    val accessibilityEnabled: Boolean,
    val notificationsEnabled: Boolean,
    /** Aplikacija je izuzeta iz optimizacije baterije (Samsung je inače "uspavljuje" preko noći). */
    val batteryUnrestricted: Boolean = true
) {
    val allCriticalGranted: Boolean
        get() = hasUsageAccess && canDrawOverlay && accessibilityEnabled

    /** Sve što treba da praćenje i blokade rade bez prekida, i preko noći. */
    val setupComplete: Boolean
        get() = allCriticalGranted && batteryUnrestricted

    val missingSetupSteps: Int
        get() = listOf(hasUsageAccess, canDrawOverlay, accessibilityEnabled, batteryUnrestricted).count { !it }
}

class PermissionStateChecker(private val context: Context) {
    fun snapshot(): PermissionState {
        return PermissionState(
            hasUsageAccess = hasUsageAccess(),
            canDrawOverlay = Settings.canDrawOverlays(context),
            accessibilityEnabled = isAccessibilityEnabled(),
            notificationsEnabled = notificationsEnabled(),
            batteryUnrestricted = context.getSystemService(PowerManager::class.java)
                ?.isIgnoringBatteryOptimizations(context.packageName) != false
        )
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun notificationsEnabled(): Boolean = PlatformCompat.notificationsGranted(context)

    fun isAccessibilityEnabled(): Boolean {
        val expected = ComponentName(context, ShortsAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    companion object {
        fun overlaySettingsUri(context: Context): Uri = Uri.parse("package:${context.packageName}")
    }
}
