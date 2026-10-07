package com.example.screenmanager.ui.feature.limits

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.screenmanager.model.AppLimitRule
import com.example.screenmanager.model.EmergencySessionConfig
import com.example.screenmanager.model.ScheduleRule
import com.example.screenmanager.model.SessionLimitRule
import com.example.screenmanager.ui.common.formatClock
import com.example.screenmanager.ui.common.formatMinutes
import com.example.screenmanager.ui.common.rememberNow
import com.example.screenmanager.ui.components.AppCard
import com.example.screenmanager.ui.components.AppScreen
import com.example.screenmanager.ui.components.AppSwitch
import com.example.screenmanager.ui.components.CardDivider
import com.example.screenmanager.ui.components.IconBadge
import com.example.screenmanager.ui.components.ListRow
import com.example.screenmanager.ui.components.ScreenContentPadding
import com.example.screenmanager.ui.components.SecondaryButton
import com.example.screenmanager.ui.components.SectionHeader
import com.example.screenmanager.ui.components.StepperRow
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing

/** Stateful ulaz u Limits tab: povezuje [LimitsViewModel] sa [LimitsScreen]. */
@Composable
fun LimitsRoute(
    onEditRule: (RuleTarget) -> Unit,
    viewModel: LimitsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    LimitsScreen(
        state = state,
        labelFor = viewModel::appLabel,
        onEditRule = onEditRule,
        actions = LimitsActions(
            onToggleAppLimit = { rule, enabled -> viewModel.saveAppLimit(rule.copy(isEnabled = enabled)) },
            onToggleSessionLimit = { rule, enabled -> viewModel.saveSessionLimit(rule.copy(isEnabled = enabled)) },
            onToggleSchedule = { rule, enabled -> viewModel.saveSchedule(rule.copy(isEnabled = enabled)) },
            onToggleShorts = { enabled -> state?.let { viewModel.saveShorts(it.shorts.copy(isEnabled = enabled)) } },
            onToggleMorningLock = { enabled ->
                state?.let { viewModel.saveMorningLock(it.morningLock.copy(isEnabled = enabled)) }
            },
            onEmergencyDurationChange = viewModel::setEmergencyDuration,
            onStartEmergency = viewModel::startEmergencyPause,
            onEndEmergency = viewModel::endEmergencyPause
        )
    )
}

/**
 * Limits tab: sva pravila na jednom mestu, grupisana po vrsti. Switch
 * uključuje/isključuje pravilo, klik na red otvara formu za izmenu.
 */
@Composable
fun LimitsScreen(
    state: LimitsUiState?,
    labelFor: (String) -> String,
    onEditRule: (RuleTarget) -> Unit,
    actions: LimitsActions
) {
    AppScreen(title = "Limits") {
        if (state == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppTheme.colors.accent)
            }
            return@AppScreen
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = ScreenContentPadding,
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item(key = "daily") {
                RuleSection(
                    kind = RuleKind.DailyLimit,
                    onAdd = { onEditRule(RuleTarget.AppLimit()) },
                    rows = state.appLimits.map { rule ->
                        RuleRowModel(
                            key = rule.id,
                            title = rule.name,
                            summary = rule.summary(labelFor),
                            enabled = rule.isEnabled,
                            onToggle = { actions.onToggleAppLimit(rule, it) },
                            onClick = { onEditRule(RuleTarget.AppLimit(ruleId = rule.id)) }
                        )
                    }
                )
            }
            item(key = "sessions") {
                RuleSection(
                    kind = RuleKind.SessionLimit,
                    onAdd = { onEditRule(RuleTarget.SessionLimit()) },
                    rows = state.sessionLimits.map { rule ->
                        RuleRowModel(
                            key = rule.id,
                            title = rule.name,
                            summary = rule.summary(labelFor),
                            enabled = rule.isEnabled,
                            onToggle = { actions.onToggleSessionLimit(rule, it) },
                            onClick = { onEditRule(RuleTarget.SessionLimit(ruleId = rule.id)) }
                        )
                    }
                )
            }
            item(key = "schedules") {
                RuleSection(
                    kind = RuleKind.Schedule,
                    onAdd = { onEditRule(RuleTarget.Schedule()) },
                    rows = state.schedules.map { rule ->
                        RuleRowModel(
                            key = rule.id,
                            title = rule.name,
                            summary = rule.summary(labelFor),
                            enabled = rule.isEnabled,
                            onToggle = { actions.onToggleSchedule(rule, it) },
                            onClick = { onEditRule(RuleTarget.Schedule(ruleId = rule.id)) }
                        )
                    }
                )
            }
            item(key = "shorts") {
                RuleSection(
                    kind = RuleKind.Shorts,
                    onAdd = null,
                    rows = listOf(
                        RuleRowModel(
                            key = "shorts",
                            title = state.shorts.mode.label,
                            summary = "${state.shorts.summary()} · ${appsSummary(state.shorts.selectedAppIds, labelFor)}",
                            enabled = state.shorts.isEnabled,
                            onToggle = actions.onToggleShorts,
                            onClick = { onEditRule(RuleTarget.Shorts()) }
                        )
                    )
                )
            }
            item(key = "morning") {
                RuleSection(
                    kind = RuleKind.MorningLock,
                    onAdd = null,
                    rows = listOf(
                        RuleRowModel(
                            key = "morning",
                            title = "After ${state.morningLock.inactivityHours}h of inactivity",
                            summary = state.morningLock.summary(labelFor),
                            enabled = state.morningLock.isEnabled,
                            onToggle = actions.onToggleMorningLock,
                            onClick = { onEditRule(RuleTarget.MorningLock) }
                        )
                    )
                )
            }
            item(key = "emergency") {
                Spacer(Modifier.height(Spacing.sm))
                EmergencyPauseCard(
                    config = state.emergency,
                    onDurationChange = actions.onEmergencyDurationChange,
                    onStart = actions.onStartEmergency,
                    onEnd = actions.onEndEmergency
                )
            }
        }
    }
}

/** Korisničke akcije na Limits tabu (da potpis ekrana ostane čitljiv). */
class LimitsActions(
    val onToggleAppLimit: (AppLimitRule, Boolean) -> Unit,
    val onToggleSessionLimit: (SessionLimitRule, Boolean) -> Unit,
    val onToggleSchedule: (ScheduleRule, Boolean) -> Unit,
    val onToggleShorts: (Boolean) -> Unit,
    val onToggleMorningLock: (Boolean) -> Unit,
    val onEmergencyDurationChange: (Int) -> Unit,
    val onStartEmergency: () -> Unit,
    val onEndEmergency: () -> Unit
)

/** Jedan red u listi pravila, nezavisno od vrste pravila. */
private class RuleRowModel(
    val key: String,
    val title: String,
    val summary: String,
    val enabled: Boolean,
    val onToggle: (Boolean) -> Unit,
    val onClick: () -> Unit
)

/**
 * Sekcija jedne vrste pravila: naslov (+ "Add" ako može više pravila te
 * vrste), kratak opis i kartica sa pravilima.
 */
@Composable
private fun RuleSection(
    kind: RuleKind,
    onAdd: (() -> Unit)?,
    rows: List<RuleRowModel>
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionHeader(
            title = kind.title,
            actionLabel = if (onAdd != null) "Add" else null,
            onAction = onAdd
        )
        AppCard(contentPadding = PaddingValues(0.dp)) {
            if (rows.isEmpty()) {
                EmptyRuleRow(icon = kind.icon, text = kind.description)
            } else {
                rows.forEachIndexed { index, row ->
                    if (index > 0) CardDivider()
                    key(row.key) { RuleRow(kind = kind, row = row) }
                }
            }
        }
    }
}

@Composable
private fun RuleRow(kind: RuleKind, row: RuleRowModel) {
    ListRow(
        title = row.title,
        subtitle = row.summary,
        onClick = row.onClick,
        leading = {
            IconBadge(
                icon = kind.icon,
                tint = if (row.enabled) AppTheme.colors.accent else AppTheme.colors.textMuted
            )
        },
        trailing = { AppSwitch(checked = row.enabled, onCheckedChange = row.onToggle) }
    )
}

@Composable
private fun EmptyRuleRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon = icon, tint = AppTheme.colors.textMuted)
        Spacer(Modifier.width(Spacing.md))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Privremeno gasi SVA pravila na zadato vreme. */
@Composable
private fun EmergencyPauseCard(
    config: EmergencySessionConfig,
    onDurationChange: (Int) -> Unit,
    onStart: () -> Unit,
    onEnd: () -> Unit
) {
    val now by rememberNow()
    val isActive = config.isActiveAt(now)
    val activeUntil = config.activeUntilMillis

    AppCard(contentPadding = PaddingValues(0.dp)) {
        ListRow(
            title = "Emergency pause",
            subtitle = if (isActive && activeUntil != null) {
                "All limits are paused until ${formatClock(activeUntil)}."
            } else {
                "Pause every limit for a short time when you really need your phone."
            },
            leading = {
                IconBadge(
                    icon = Icons.Rounded.PauseCircle,
                    tint = if (isActive) AppTheme.colors.warning else AppTheme.colors.textSecondary
                )
            }
        )
        CardDivider()
        if (isActive) {
            SecondaryButton(
                text = "End pause now",
                onClick = onEnd,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg)
            )
        } else {
            StepperRow(
                label = "Duration",
                value = config.defaultDurationMinutes,
                onValueChange = onDurationChange,
                range = 5..120,
                step = 5,
                valueLabel = ::formatMinutes
            )
            SecondaryButton(
                text = "Pause all limits",
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg)
            )
        }
    }
}
