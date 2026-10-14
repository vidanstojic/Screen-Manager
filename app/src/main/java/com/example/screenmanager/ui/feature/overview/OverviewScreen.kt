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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.screenmanager.domain.PermissionState
import com.example.screenmanager.model.ShortsMode
import com.example.screenmanager.ui.AppViewModel
import com.example.screenmanager.ui.common.OnResume
import com.example.screenmanager.ui.common.formatClock
import com.example.screenmanager.ui.common.formatDuration
import com.example.screenmanager.ui.common.pluralize
import com.example.screenmanager.ui.common.rememberCountUp
import com.example.screenmanager.ui.components.AppCard
import com.example.screenmanager.ui.components.AppScreen
import com.example.screenmanager.ui.components.AppUsageRow
import com.example.screenmanager.ui.components.CardDivider
import com.example.screenmanager.ui.components.ChevronIcon
import com.example.screenmanager.ui.components.CircleIconButton
import com.example.screenmanager.ui.components.IconBadge
import com.example.screenmanager.ui.components.ListRow
import com.example.screenmanager.ui.components.ProgressRing
import com.example.screenmanager.ui.components.SectionHeader
import com.example.screenmanager.ui.components.StatusPill
import com.example.screenmanager.ui.components.TextAction
import com.example.screenmanager.ui.components.screenContentPadding
import com.example.screenmanager.ui.feature.limits.ProtectionSummary
import com.example.screenmanager.ui.feature.limits.RuleKind
import com.example.screenmanager.ui.feature.limits.label
import com.example.screenmanager.ui.model.AppUsageItem
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing
import java.time.LocalTime
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
                showBadge = !permissions.setupComplete
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = screenContentPadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            if (!permissions.setupComplete) {
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
                ScreenTimeCard(state = state, onClick = onOpenStats, onSetGoal = onOpenSettings)
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
    val missing = permissions.missingSetupSteps

    AppCard(
        onClick = onClick,
        borderColor = AppTheme.colors.warning.copy(alpha = 0.45f),
        contentPadding = PaddingValues(0.dp)
    ) {
        ListRow(
            title = "Finish setup",
            subtitle = "${pluralize(missing, "setting")} still needed to track and block apps reliably.",
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

/**
 * Glavna kartica: ukupno vreme danas (odbrojava se pri otvaranju), prsten
 * dnevnog cilja, poređenje sa jučerašnjim danom i mini grafik po satima.
 * Bez postavljenog cilja umesto prstena nudi link ka podešavanju cilja.
 */
@Composable
private fun ScreenTimeCard(state: OverviewUiState, onClick: () -> Unit, onSetGoal: () -> Unit) {
    val colors = AppTheme.colors
    val shownMs = rememberCountUp(state.todayMs)
    val goalMs = state.dailyGoalMs
    val numberStyle = (if (goalMs != null) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayMedium)
        .copy(brush = Brush.linearGradient(listOf(colors.textPrimary, lerp(colors.textPrimary, colors.accent, 0.55f))))

    AppCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        contentPadding = PaddingValues(Spacing.xl)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (goalMs != null) {
                GoalRing(todayMs = state.todayMs, goalMs = goalMs)
                Spacer(Modifier.width(Spacing.lg))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "SCREEN TIME TODAY", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                Spacer(Modifier.height(Spacing.xs))
                Text(text = formatDuration(shownMs), style = numberStyle, maxLines = 1)
                Spacer(Modifier.height(Spacing.sm))
                TrendPill(todayMs = state.todayMs, yesterdaySameTimeMs = state.yesterdaySameTimeMs)
            }
        }
        if (goalMs == null) {
            Spacer(Modifier.height(Spacing.sm))
            TextAction(text = "Set a daily goal", onClick = onSetGoal)
        }
        Spacer(Modifier.height(Spacing.lg))
        HourlySparkBars(hourlyMs = state.hourlyMs)
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("00", "06", "12", "18", "24").forEach { label ->
                Text(text = label, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
        }
    }
}

/** Prsten: koliko je dnevnog cilja potrošeno. Preko cilja prelazi u boje upozorenja. */
@Composable
private fun GoalRing(todayMs: Long, goalMs: Long) {
    val colors = AppTheme.colors
    val ratio = todayMs.toFloat() / goalMs
    val overGoal = ratio > 1f
    val progress = rememberCountUp(ratio.coerceAtMost(1f))
    ProgressRing(
        progress = progress,
        size = 118.dp,
        strokeWidth = 12.dp,
        colors = if (overGoal) listOf(colors.warning, colors.danger) else listOf(colors.accent, colors.highlight)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (overGoal) "+${formatDuration(todayMs - goalMs)}" else "${(ratio * 100).roundToInt()}%",
                style = MaterialTheme.typography.titleLarge,
                color = if (overGoal) colors.warning else colors.textPrimary,
                maxLines = 1
            )
            Text(
                text = if (overGoal) "over goal" else "of ${formatDuration(goalMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1
            )
        }
    }
}

/** "↓18% vs yesterday" — poredi se sa jučerašnjim danom DO ISTOG doba dana. */
@Composable
private fun TrendPill(todayMs: Long, yesterdaySameTimeMs: Long?) {
    if (yesterdaySameTimeMs == null) {
        StatusPill(text = "No data from yesterday")
        return
    }
    val changePercent = ((todayMs - yesterdaySameTimeMs) * 100.0 / yesterdaySameTimeMs).roundToInt()
    when {
        abs(changePercent) < 3 -> StatusPill(text = "Same as yesterday")
        changePercent < 0 -> StatusPill(
            text = "${-changePercent}% vs yesterday",
            color = AppTheme.colors.success,
            icon = Icons.AutoMirrored.Rounded.TrendingDown
        )
        else -> StatusPill(
            text = "$changePercent% vs yesterday",
            color = AppTheme.colors.warning,
            icon = Icons.AutoMirrored.Rounded.TrendingUp
        )
    }
}

/** Mini grafik: 24 stubića (po jedan za svaki sat), bez osa; trenutni sat je istaknut. */
@Composable
private fun HourlySparkBars(hourlyMs: List<Long>, modifier: Modifier = Modifier) {
    val barTop = AppTheme.colors.accent
    val barBottom = AppTheme.colors.accentEnd
    val nowTop = AppTheme.colors.highlight
    val emptyColor = AppTheme.colors.surfaceRaised
    val maxMs = (hourlyMs.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val currentHour = remember { LocalTime.now().hour }
    val growth = rememberCountUp(1f, durationMillis = 800)

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
            val barHeight = (size.height * value / maxMs * growth).coerceAtLeast(minHeight)
            val topLeft = Offset(slotWidth * index + (slotWidth - barWidth) / 2f, size.height - barHeight)
            val barSize = Size(barWidth, barHeight)
            val corner = CornerRadius(barWidth / 2f, barWidth / 2f)
            if (value > 0) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(if (index == currentHour) nowTop else barTop, barBottom),
                        startY = topLeft.y,
                        endY = size.height
                    ),
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

/** Sažetak uključenih pravila po vrsti, kao mreža pločica; svaka vodi na Limits tab. */
@Composable
private fun ProtectionSection(
    protection: ProtectionSummary,
    morningLockUntil: Long?,
    onManage: () -> Unit
) {
    val tiles = listOf(
        ProtectionTileModel(RuleKind.DailyLimit, "Daily", activeCountLabel(protection.dailyLimits), protection.dailyLimits > 0),
        ProtectionTileModel(
            RuleKind.SessionLimit, "Sessions", activeCountLabel(protection.sessionLimits), protection.sessionLimits > 0
        ),
        ProtectionTileModel(RuleKind.Schedule, "Schedule", activeCountLabel(protection.schedules), protection.schedules > 0),
        ProtectionTileModel(RuleKind.Shorts, "Shorts", shortsLabel(protection.shortsMode), protection.shortsMode != null),
        ProtectionTileModel(
            kind = RuleKind.MorningLock,
            title = "Morning",
            status = when {
                morningLockUntil != null -> "Until ${formatClock(morningLockUntil)}"
                protection.morningLockEnabled -> "On"
                else -> "Off"
            },
            active = protection.morningLockEnabled
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionHeader(title = "Protection", actionLabel = "Manage", onAction = onManage)
        // 3 + 2 pločice: prvi red su vremenska pravila, drugi sadržaj i jutro.
        listOf(tiles.take(3), tiles.drop(3)).forEach { rowTiles ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                rowTiles.forEach { tile ->
                    ProtectionTile(tile = tile, onClick = onManage, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private class ProtectionTileModel(
    val kind: RuleKind,
    val title: String,
    val status: String,
    val active: Boolean
)

@Composable
private fun ProtectionTile(tile: ProtectionTileModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    AppCard(
        modifier = modifier,
        onClick = onClick,
        tint = if (tile.active) {
            Brush.verticalGradient(listOf(colors.accentStart.copy(alpha = 0.16f), Color.Transparent))
        } else {
            null
        },
        contentPadding = PaddingValues(Spacing.md)
    ) {
        IconBadge(
            icon = tile.kind.icon,
            tint = if (tile.active) colors.accent else colors.textMuted,
            size = 32.dp
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = tile.title,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = tile.status,
            style = MaterialTheme.typography.titleSmall,
            color = if (tile.active) colors.textPrimary else colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun activeCountLabel(count: Int): String = if (count == 0) "Off" else "$count active"

private fun shortsLabel(mode: ShortsMode?): String = mode?.label ?: "Off"
