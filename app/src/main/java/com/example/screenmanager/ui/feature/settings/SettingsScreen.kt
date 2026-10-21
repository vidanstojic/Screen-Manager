package com.example.screenmanager.ui.feature.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.screenmanager.domain.PermissionState
import com.example.screenmanager.domain.PermissionStateChecker
import com.example.screenmanager.ui.AppViewModel
import com.example.screenmanager.ui.common.formatMinutes
import com.example.screenmanager.ui.components.AccessibilityDisclosureDialog
import com.example.screenmanager.ui.components.AppCard
import com.example.screenmanager.ui.components.AppScreen
import com.example.screenmanager.ui.components.CardDivider
import com.example.screenmanager.ui.components.IconBadge
import com.example.screenmanager.ui.components.ListRow
import com.example.screenmanager.ui.components.openAccessibilitySettings
import com.example.screenmanager.ui.components.SectionHeader
import com.example.screenmanager.ui.components.StepperRow
import com.example.screenmanager.ui.components.SwitchRow
import com.example.screenmanager.ui.components.TextAction
import com.example.screenmanager.ui.components.screenContentPadding
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing

/** Stateful ulaz u Settings ekran. */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    appViewModel: AppViewModel = viewModel(),
    viewModel: SettingsViewModel = viewModel()
) {
    val permissions by appViewModel.permissionState.collectAsStateWithLifecycle()
    val backgroundStopped by appViewModel.backgroundStopped.collectAsStateWithLifecycle()
    val dailyGoalMinutes by viewModel.dailyGoalMinutes.collectAsStateWithLifecycle()
    SettingsScreen(
        permissions = permissions,
        backgroundStopped = backgroundStopped,
        onDismissBackgroundStopped = appViewModel::acknowledgeBackgroundStop,
        dailyGoalMinutes = dailyGoalMinutes,
        onDailyGoalEnabledChange = viewModel::setDailyGoalEnabled,
        onDailyGoalChange = viewModel::setDailyGoalMinutes,
        onBack = onBack
    )
}

/**
 * Settings: dnevni cilj, sistemske dozvole (TRS §3) i informacije o aplikaciji.
 * Stanje dozvola se osvežava samo po povratku iz sistemskih podešavanja
 * (ON_RESUME → AppViewModel).
 */
@Composable
fun SettingsScreen(
    permissions: PermissionState,
    backgroundStopped: Boolean,
    onDismissBackgroundStopped: () -> Unit,
    dailyGoalMinutes: Int?,
    onDailyGoalEnabledChange: (Boolean) -> Unit,
    onDailyGoalChange: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showAccessibilityDisclosure by rememberSaveable { mutableStateOf(false) }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    if (showPrivacy) PrivacyDialog(onDismiss = { showPrivacy = false })
    if (showAccessibilityDisclosure) {
        AccessibilityDisclosureDialog(
            onAgree = {
                showAccessibilityDisclosure = false
                context.openAccessibilitySettings()
            },
            onDismiss = { showAccessibilityDisclosure = false }
        )
    }

    AppScreen(title = "Settings", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(screenContentPadding()),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            if (backgroundStopped) BackgroundStoppedCard(onDismiss = onDismissBackgroundStopped)

            SectionHeader(title = "Daily goal")
            AppCard(contentPadding = PaddingValues(0.dp)) {
                SwitchRow(
                    title = "Screen time goal",
                    subtitle = "Shows a ring on the Screen Manager overview: how much of your daily goal you have used.",
                    checked = dailyGoalMinutes != null,
                    onCheckedChange = onDailyGoalEnabledChange,
                    leading = { IconBadge(icon = Icons.Rounded.TrackChanges) }
                )
                if (dailyGoalMinutes != null) {
                    CardDivider()
                    StepperRow(
                        label = "Goal per day",
                        value = dailyGoalMinutes,
                        onValueChange = onDailyGoalChange,
                        range = 30..720,
                        step = 30,
                        valueLabel = ::formatMinutes
                    )
                }
            }

            SectionHeader(title = "Permissions", modifier = Modifier.padding(top = Spacing.md))
            AppCard(contentPadding = PaddingValues(0.dp)) {
                PermissionRow(
                    icon = Icons.Rounded.QueryStats,
                    title = "Usage access",
                    description = "Measures screen time and detects which app is open.",
                    granted = permissions.hasUsageAccess,
                    onGrant = { context.openSettings(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                )
                CardDivider()
                PermissionRow(
                    icon = Icons.Rounded.Layers,
                    title = "Display over other apps",
                    description = "Shows the block screen on top of a limited app.",
                    granted = permissions.canDrawOverlay,
                    onGrant = {
                        context.openSettings(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                PermissionStateChecker.overlaySettingsUri(context)
                            )
                        )
                    }
                )
                CardDivider()
                PermissionRow(
                    icon = Icons.Rounded.Accessibility,
                    title = "Accessibility service",
                    description = "Detects Shorts and Reels inside YouTube and Instagram.",
                    granted = permissions.accessibilityEnabled,
                    onGrant = { showAccessibilityDisclosure = true }
                )
                CardDivider()
                PermissionRow(
                    icon = Icons.Rounded.BatteryAlert,
                    title = "Unrestricted battery",
                    description = "Keeps tracking, limits and the morning lock running overnight. " +
                        "Find Focus Flow in the list and choose \"Don't optimize\" / \"Unrestricted\".",
                    granted = permissions.batteryUnrestricted,
                    onGrant = { context.openSettings(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                )
                if (BackgroundActivity.isXiaomi) {
                    CardDivider()
                    XiaomiBackgroundRows()
                }
                CardDivider()
                PermissionRow(
                    icon = Icons.Rounded.Notifications,
                    title = "Notifications",
                    description = "Needed for alarms and the monitoring status notification.",
                    granted = permissions.notificationsEnabled,
                    onGrant = {
                        context.openSettings(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        )
                    }
                )
            }

            SectionHeader(title = "About", modifier = Modifier.padding(top = Spacing.md))
            AppCard(contentPadding = PaddingValues(0.dp)) {
                val versionName = remember { context.appVersionName() }
                ListRow(
                    title = "Focus Flow",
                    subtitle = "Version $versionName",
                    leading = { IconBadge(icon = Icons.Rounded.Shield) }
                )
                CardDivider()
                ListRow(
                    title = "Privacy",
                    subtitle = "Your data stays on this device. Nothing is collected or shared.",
                    onClick = { showPrivacy = true }
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    onGrant: () -> Unit
) {
    ListRow(
        title = title,
        subtitle = description,
        onClick = if (granted) null else onGrant,
        leading = {
            IconBadge(icon = icon, tint = if (granted) AppTheme.colors.success else AppTheme.colors.warning)
        },
        trailing = {
            if (granted) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Granted",
                    tint = AppTheme.colors.success
                )
            } else {
                TextAction(text = "Allow", onClick = onGrant)
            }
        }
    )
}

private fun Context.openSettings(intent: Intent) {
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun Context.appVersionName(): String =
    runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: "1.0"
