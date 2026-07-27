package com.example.screenmanager.domain

import android.Manifest
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.screenmanager.service.ShortsAccessibilityService

data class PermissionState(
    val hasUsageAccess: Boolean,
    val canDrawOverlay: Boolean,
    val accessibilityEnabled: Boolean,
    val notificationsEnabled: Boolean
) {
    val allCriticalGranted: Boolean
        get() = hasUsageAccess && canDrawOverlay && accessibilityEnabled
}

class PermissionStateChecker(private val context: Context) {
    fun snapshot(): PermissionState {
        return PermissionState(
            hasUsageAccess = hasUsageAccess(),
            canDrawOverlay = Settings.canDrawOverlays(context),
            accessibilityEnabled = isAccessibilityEnabled(),
            notificationsEnabled = notificationsEnabled()
        )
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun notificationsEnabled(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun isAccessibilityEnabled(): Boolean {
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
