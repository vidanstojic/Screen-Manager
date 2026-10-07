package com.example.screenmanager.ui.feature.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.screenmanager.ui.AppViewModel
import com.example.screenmanager.ui.common.OnResume
import com.example.screenmanager.ui.common.formatClock
import com.example.screenmanager.ui.common.formatDuration
import com.example.screenmanager.ui.common.pluralize
import com.example.screenmanager.ui.common.relativeDayLabel
import com.example.screenmanager.ui.common.rememberNow
import com.example.screenmanager.ui.components.AppCard
import com.example.screenmanager.ui.components.AppScreen
import com.example.screenmanager.ui.components.ChevronIcon
import com.example.screenmanager.ui.components.CircleIconButton
import com.example.screenmanager.ui.components.IconBadge
import com.example.screenmanager.ui.components.ScreenContentPadding
import com.example.screenmanager.ui.components.StatusPill
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId

/** Stateful ulaz u početni ekran. */
@Composable
fun LauncherRoute(
    onOpenScreenManager: () -> Unit,
    onOpenAlarms: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: LauncherViewModel = viewModel(),
    appViewModel: AppViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val permissions by appViewModel.permissionState.collectAsState()
    OnResume(viewModel::onResume)

    LauncherScreen(
        state = state,
        setupIncomplete = !permissions.allCriticalGranted,
        onOpenScreenManager = onOpenScreenManager,
        onOpenAlarms = onOpenAlarms,
        onOpenSettings = onOpenSettings
    )
}

/**
 * Početni ekran aplikacije: korisnik bira u koji deo ulazi — Screen Manager
 * ili Smart Alarms. Svaka kartica pokazuje kratak status tog dela.
 */
@Composable
fun LauncherScreen(
    state: LauncherUiState,
    setupIncomplete: Boolean,
    onOpenScreenManager: () -> Unit,
    onOpenAlarms: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val now by rememberNow()

    AppScreen(
        title = "Focus Flow",
        actions = {
            CircleIconButton(
                icon = Icons.Rounded.Settings,
                contentDescription = "Settings",
                onClick = onOpenSettings,
                showBadge = setupIncomplete
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(ScreenContentPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Text(
                text = "${greeting(now)}. Where to?",
                style = MaterialTheme.typography.bodyLarge,
                color = AppTheme.colors.textSecondary
            )

            ModuleCard(
                icon = Icons.Rounded.Shield,
                tint = AppTheme.colors.accent,
                title = "Screen Manager",
                description = "See your screen time, set app limits and block Shorts & Reels.",
                status = listOf(
                    "${formatDuration(state.todayMs)} today",
                    if (state.activeRules > 0) "${pluralize(state.activeRules, "limit")} active" else "No limits active"
                ),
                onClick = onOpenScreenManager
            )

            ModuleCard(
                icon = Icons.Rounded.Alarm,
                tint = AppTheme.colors.warning,
                title = "Smart Alarms",
                description = "Wake-up alarms. Dismissing one can start your morning lock.",
                status = listOf(
                    when {
                        state.nextAlarmAt != null ->
                            "Next: ${formatClock(state.nextAlarmAt)} · ${relativeDayLabel(state.nextAlarmAt, now)}"
                        state.alarmCount > 0 -> "All alarms are off"
                        else -> "No alarms yet"
                    }
                ),
                onClick = onOpenAlarms
            )
        }
    }
}

/** Velika kartica jednog dela aplikacije: ikonica, naziv, opis i status. */
@Composable
private fun ModuleCard(
    icon: ImageVector,
    tint: Color,
    title: String,
    description: String,
    status: List<String>,
    onClick: () -> Unit
) {
    AppCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        contentPadding = PaddingValues(Spacing.xl)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = icon, tint = tint, size = 56.dp)
            Spacer(Modifier.weight(1f))
            ChevronIcon()
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = AppTheme.colors.textPrimary
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary
        )
        Spacer(Modifier.height(Spacing.lg))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            status.forEach { line -> StatusPill(text = line, color = tint) }
        }
    }
}

private fun greeting(now: Long): String =
    when (Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
