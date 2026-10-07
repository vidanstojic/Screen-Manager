package com.example.screenmanager.ui.feature.overview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.screenmanager.domain.PermissionState
import com.example.screenmanager.model.ShortsMode
import com.example.screenmanager.ui.AppViewModel
import com.example.screenmanager.ui.common.OnResume
import com.example.screenmanager.ui.common.formatClock
import com.example.screenmanager.ui.common.formatDuration
import com.example.screenmanager.ui.common.pluralize
import com.example.screenmanager.ui.components.AppCard
import com.example.screenmanager.ui.components.AppScreen
import com.example.screenmanager.ui.components.AppUsageRow
import com.example.screenmanager.ui.components.CardDivider
import com.example.screenmanager.ui.components.ChevronIcon
import com.example.screenmanager.ui.components.CircleIconButton
import com.example.screenmanager.ui.components.IconBadge
import com.example.screenmanager.ui.components.ListRow
import com.example.screenmanager.ui.components.ScreenContentPadding
import com.example.screenmanager.ui.components.SectionHeader
import com.example.screenmanager.ui.components.StatusPill
import com.example.screenmanager.ui.components.TextAction
import com.example.screenmanager.ui.feature.limits.ProtectionSummary
import com.example.screenmanager.ui.feature.limits.RuleKind
import com.example.screenmanager.ui.feature.limits.label
import com.example.screenmanager.ui.model.AppUsageItem
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing
import kotlin.math.abs
import kotlin.math.roundToInt

/** Stateful ulaz u Overview tab. */
@Composable
fun OverviewRoute(
    onBack: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenLimits: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenApp: (AppUsageItem) -> Unit,
    viewModel: OverviewViewModel = viewModel(),
    appViewModel: AppViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val permissions by appViewModel.permissionState.collectAsState()
    OnResume(viewModel::onResume)

    OverviewScreen(
        state = state,
        permissions = permissions,
        onBack = onBack,
        onOpenStats = onOpenStats,
        onOpenLimits = onOpenLimits,
        onOpenSettings = onOpenSettings,
        onOpenApp = onOpenApp,
        onEndEmergencyPause = viewModel::endEmergencyPause
    )
}

/**
 * Overview tab — prvi ekran Screen Manager-a: današnje vreme, najkorišćenije
 * aplikacije i stanje zaštite. Svaka kartica vodi na tab koji tu temu
 * prikazuje detaljno. Strelica nazad vraća na početni ekran aplikacije.
 */
@Composable
fun OverviewScreen(
    state: OverviewUiState,
    permissions: PermissionState,
    onBack: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenLimits: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenApp: (AppUsageItem) -> Unit,
    onEndEmergencyPause: () -> Unit
) {
    AppScreen(
        title = "Screen Manager",
        onBack = onBack,
        largeTitle = true,
        hasBottomNav = true,
        actions = {
            CircleIconButton(
                icon = Icons.Rounded.Settings,
                contentDescription = "Settings",
                onClick = onOpenSettings,
                showBadge = !permissions.allCriticalGranted
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = ScreenContentPadding,
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            if (!permissions.allCriticalGranted) {
                item(key = "permissions") {
                    PermissionsBanner(permissions = permissions, onClick = onOpenSettings)
                }
            }
            if (state.emergencyPauseUntil != null) {
                item(key = "emergency") {
                    EmergencyBanner(until = state.emergencyPauseUntil, onEnd = onEndEmergencyPause)
                }
            }
            item(key = "today") {
                ScreenTimeCard(state = state, onClick = onOpenStats)
            }
            item(key = "top-apps") {
                TopAppsSection(apps = state.topApps, onOpenApp = onOpenApp, onSeeAll = onOpenStats)
            }
            item(key = "protection") {
                ProtectionSection(
                    protection = state.protection,
                    morningLockUntil = state.morningLockUntil,
                    onManage = onOpenLimits
                )
            }
        }
    }
}

/** Upozorenje da nedostaju dozvole bez kojih praćenje/blokiranje ne radi. */
@Composable
private fun PermissionsBanner(permissions: PermissionState, onClick: () -> Unit) {
    val missing = listOf(
        permissions.hasUsageAccess,
        permissions.canDrawOverlay,
        permissions.accessibilityEnabled
    ).count { !it }

    AppCard(
        onClick = onClick,
        borderColor = AppTheme.colors.warning.copy(alpha = 0.45f),
        contentPadding = PaddingValues(0.dp)
    ) {
        ListRow(
            title = "Finish setup",
            subtitle = "${pluralize(missing, "permission")} still needed to track and block apps.",
            leading = { IconBadge(icon = Icons.Rounded.WarningAmber, tint = AppTheme.colors.warning) },
            trailing = { ChevronIcon() }
        )
    }
}

@Composable
private fun EmergencyBanner(until: Long, onEnd: () -> Unit) {
    AppCard(
        borderColor = AppTheme.colors.warning.copy(alpha = 0.45f),
        contentPadding = PaddingValues(0.dp)
    ) {
        ListRow(
            title = "Emergency pause is on",
            subtitle = "All limits are paused until ${formatClock(until)}.",
            leading = { IconBadge(icon = Icons.Rounded.PauseCircle, tint = AppTheme.colors.warning) },
            trailing = { TextAction(text = "End", onClick = onEnd, color = AppTheme.colors.warning) }
        )
    }
}

/** Glavna kartica: ukupno vreme danas, poređenje sa jučerašnjim danom i mini grafik po satima. */
@Composable
private fun ScreenTimeCard(state: OverviewUiState, onClick: () -> Unit) {
    AppCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        contentPadding = PaddingValues(Spacing.xl)
    ) {
        Text(
            text = "SCREEN TIME TODAY",
            style = MaterialTheme.typography.labelSmall,
            color = AppTheme.colors.textMuted
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = formatDuration(state.todayMs),
            // Blagi prelaz bele u ton akcenta daje broju dubinu.
            style = MaterialTheme.typography.displayMedium.copy(
                brush = Brush.linearGradient(
                    listOf(AppTheme.colors.textPrimary, lerp(AppTheme.colors.textPrimary, AppTheme.colors.accent, 0.55f))
                )
            )
        )
        Spacer(Modifier.height(Spacing.md))
        TrendPill(todayMs = state.todayMs, yesterdaySameTimeMs = state.yesterdaySameTimeMs)
        Spacer(Modifier.height(Spacing.xl))
        HourlySparkBars(hourlyMs = state.hourlyMs)
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("00", "06", "12", "18", "24").forEach { label ->
                Text(text = label, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
            }
        }
    }
}

/** "12% less than yesterday" — poredi se sa jučerašnjim danom DO ISTOG doba dana. */
@Composable
private fun TrendPill(todayMs: Long, yesterdaySameTimeMs: Long?) {
    if (yesterdaySameTimeMs == null) {
        StatusPill(text = "No data from yesterday to compare")
        return
    }
    val changePercent = ((todayMs - yesterdaySameTimeMs) * 100.0 / yesterdaySameTimeMs).roundToInt()
    when {
        abs(changePercent) < 3 -> StatusPill(text = "About the same as yesterday by now")
        changePercent < 0 -> StatusPill(
            text = "${-changePercent}% less than yesterday by now",
            color = AppTheme.colors.success,
            icon = Icons.AutoMirrored.Rounded.TrendingDown
        )
        else -> StatusPill(
            text = "$changePercent% more than yesterday by now",
            color = AppTheme.colors.warning,
            icon = Icons.AutoMirrored.Rounded.TrendingUp
        )
    }
}

/** Mini grafik: 24 stubića (po jedan za svaki sat), bez osa. */
@Composable
private fun HourlySparkBars(hourlyMs: List<Long>, modifier: Modifier = Modifier) {
    val barTop = AppTheme.colors.accent
    val barBottom = AppTheme.colors.accentEnd
    val emptyColor = AppTheme.colors.surfaceRaised
    val maxMs = (hourlyMs.maxOrNull() ?: 0L).coerceAtLeast(1L)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        if (hourlyMs.isEmpty()) return@Canvas
        val slotWidth = size.width / hourlyMs.size
        val barWidth = slotWidth * 0.6f
        val minHeight = 3.dp.toPx()
        hourlyMs.forEachIndexed { index, value ->
            val barHeight = (size.height * value / maxMs).coerceAtLeast(minHeight)
            val topLeft = Offset(slotWidth * index + (slotWidth - barWidth) / 2f, size.height - barHeight)
            val barSize = Size(barWidth, barHeight)
            val corner = CornerRadius(barWidth / 2f, barWidth / 2f)
            if (value > 0) {
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(barTop, barBottom), startY = topLeft.y, endY = size.height),
                    topLeft = topLeft,
                    size = barSize,
                    cornerRadius = corner
                )
            } else {
                drawRoundRect(emptyColor, topLeft, barSize, corner)
            }
        }
    }
}

@Composable
private fun TopAppsSection(
    apps: List<AppUsageItem>,
    onOpenApp: (AppUsageItem) -> Unit,
    onSeeAll: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionHeader(title = "Most used today", actionLabel = "See all", onAction = onSeeAll)
        AppCard(contentPadding = PaddingValues(0.dp)) {
            if (apps.isEmpty()) {
                Text(
                    text = "No app usage recorded yet today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary,
                    modifier = Modifier.padding(Spacing.lg)
                )
            } else {
                val maxDuration = apps.first().durationMs
                apps.forEachIndexed { index, app ->
                    if (index > 0) CardDivider()
                    AppUsageRow(app = app, maxDurationMs = maxDuration, onClick = { onOpenApp(app) })
                }
            }
        }
    }
}

/** Sažetak uključenih pravila po vrsti; cela kartica vodi na Limits tab. */
@Composable
private fun ProtectionSection(
    protection: ProtectionSummary,
    morningLockUntil: Long?,
    onManage: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionHeader(title = "Protection", actionLabel = "Manage", onAction = onManage)
        AppCard(onClick = onManage, contentPadding = PaddingValues(0.dp)) {
            ProtectionRow(RuleKind.DailyLimit, activeCountLabel(protection.dailyLimits), protection.dailyLimits > 0)
            CardDivider()
            ProtectionRow(RuleKind.SessionLimit, activeCountLabel(protection.sessionLimits), protection.sessionLimits > 0)
            CardDivider()
            ProtectionRow(RuleKind.Schedule, activeCountLabel(protection.schedules), protection.schedules > 0)
            CardDivider()
            ProtectionRow(RuleKind.Shorts, shortsLabel(protection.shortsMode), protection.shortsMode != null)
            CardDivider()
            ProtectionRow(
                kind = RuleKind.MorningLock,
                status = when {
                    morningLockUntil != null -> "Locked until ${formatClock(morningLockUntil)}"
                    protection.morningLockEnabled -> "On"
                    else -> "Off"
                },
                active = protection.morningLockEnabled
            )
        }
    }
}

@Composable
private fun ProtectionRow(kind: RuleKind, status: String, active: Boolean) {
    ListRow(
        title = kind.title,
        leading = {
            IconBadge(
                icon = kind.icon,
                tint = if (active) AppTheme.colors.accent else AppTheme.colors.textMuted,
                size = 36.dp
            )
        },
        trailing = {
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                color = if (active) AppTheme.colors.textPrimary else AppTheme.colors.textMuted
            )
        }
    )
}

private fun activeCountLabel(count: Int): String = if (count == 0) "Off" else "$count active"

private fun shortsLabel(mode: ShortsMode?): String = mode?.label ?: "Off"
