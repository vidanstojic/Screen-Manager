package com.example.screenmanager.ui.feature.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.screenmanager.ui.components.StatusPill
import com.example.screenmanager.ui.components.screenContentPadding
import com.example.screenmanager.ui.theme.AppSection
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.DarkAppColors
import com.example.screenmanager.ui.theme.GlowAlpha
import com.example.screenmanager.ui.theme.Spacing
import com.example.screenmanager.ui.theme.forSection
import com.example.screenmanager.ui.theme.glow
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
                .padding(screenContentPadding()),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            val greeting = greeting(now)
            Column {
                Text(
                    text = greeting.text,
                    style = MaterialTheme.typography.headlineSmall.copy(brush = Brush.linearGradient(greeting.colors))
                )
                Text(
                    text = "Where to?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppTheme.colors.textSecondary
                )
            }

            ModuleCard(
                section = AppSection.ScreenManager,
                icon = Icons.Rounded.Shield,
                title = "Screen Manager",
                description = "See your screen time, set app limits and block Shorts & Reels.",
                status = listOf(
                    "${formatDuration(state.todayMs)} today",
                    if (state.activeRules > 0) "${pluralize(state.activeRules, "limit")} active" else "No limits active"
                ),
                onClick = onOpenScreenManager
            )

            ModuleCard(
                section = AppSection.Alarms,
                icon = Icons.Rounded.Alarm,
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

/**
 * Velika kartica jednog dela aplikacije: gradijent u bojama tog dela, krupna
 * ikonica sa sjajem, naziv, opis i status.
 */
@Composable
private fun ModuleCard(
    section: AppSection,
    icon: ImageVector,
    title: String,
    description: String,
    status: List<String>,
    onClick: () -> Unit
) {
    val module = DarkAppColors.forSection(section)
    AppCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        tint = Brush.linearGradient(
            listOf(module.accentStart.copy(alpha = 0.55f), module.accentEnd.copy(alpha = 0.12f))
        ),
        contentPadding = PaddingValues(Spacing.xl)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .glow(module.accentStart.copy(alpha = GlowAlpha), blurRadius = 18.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(module.accentStart, module.accentEnd))),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
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
            status.forEach { line -> StatusPill(text = line, color = module.accent) }
        }
    }
}

/** Pozdrav i njegove boje prema dobu dana (jutro toplo, popodne plavo, veče ljubičasto, noć indigo). */
private data class Greeting(val text: String, val colors: List<Color>)

private fun greeting(now: Long): Greeting =
    when (Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).hour) {
        in 5..11 -> Greeting("Good morning", listOf(Color(0xFFFDE68A), Color(0xFFFB7185)))
        in 12..17 -> Greeting("Good afternoon", listOf(Color(0xFF7DD3FC), Color(0xFF818CF8)))
        in 18..21 -> Greeting("Good evening", listOf(Color(0xFFF0ABFC), Color(0xFFA78BFA)))
        else -> Greeting("Good night", listOf(Color(0xFFA5B4FC), Color(0xFF6366F1)))
    }
