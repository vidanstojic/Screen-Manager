package com.example.screenmanager.ui.feature.alarms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.screenmanager.model.AlarmRule
import com.example.screenmanager.ui.common.formatClock
import com.example.screenmanager.ui.common.rememberNow
import com.example.screenmanager.ui.components.AddFab
import com.example.screenmanager.ui.components.AppCard
import com.example.screenmanager.ui.components.AppScreen
import com.example.screenmanager.ui.components.AppSwitch
import com.example.screenmanager.ui.components.EmptyState
import com.example.screenmanager.ui.components.FabSpacer
import com.example.screenmanager.ui.components.ScreenContentPadding
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing

/** Stateful ulaz u Alarms tab. */
@Composable
fun AlarmsRoute(
    onEditAlarm: (alarmId: Long?) -> Unit,
    viewModel: AlarmsViewModel = viewModel()
) {
    val alarms by viewModel.alarms.collectAsState()
    val now by rememberNow()
    val loadedAlarms = alarms.orEmpty()

    AlarmsScreen(
        alarms = loadedAlarms,
        isLoading = alarms == null,
        nextAlarmAt = loadedAlarms.filter { it.enabled }.minOfOrNull { viewModel.nextTriggerAt(it, now) },
        now = now,
        onToggle = viewModel::setAlarmEnabled,
        onEditAlarm = onEditAlarm
    )
}

/** Alarms tab: lista alarma; klik otvara formu, "+" dodaje novi. */
@Composable
fun AlarmsScreen(
    alarms: List<AlarmRule>,
    isLoading: Boolean,
    nextAlarmAt: Long?,
    now: Long,
    onToggle: (AlarmRule, Boolean) -> Unit,
    onEditAlarm: (alarmId: Long?) -> Unit
) {
    AppScreen(title = "Alarms") {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = ScreenContentPadding,
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            if (alarms.isNotEmpty()) {
                item(key = "next") {
                    Text(
                        text = if (nextAlarmAt != null) {
                            "Next alarm ${formatTimeUntil(nextAlarmAt - now)}"
                        } else {
                            "All alarms are off"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier.padding(bottom = Spacing.xs)
                    )
                }
            }
            items(alarms, key = { it.id }) { alarm ->
                AlarmCard(
                    alarm = alarm,
                    onToggle = { onToggle(alarm, it) },
                    onClick = { onEditAlarm(alarm.id) }
                )
            }
            if (alarms.isEmpty() && !isLoading) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Rounded.Alarm,
                        title = "No alarms yet",
                        message = "Add a wake-up alarm. Dismissing it can start your morning lock."
                    )
                }
            }
            item(key = "fab-spacer") { FabSpacer() }
        }

        AddFab(
            contentDescription = "Add alarm",
            icon = Icons.Rounded.Add,
            onClick = { onEditAlarm(null) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Spacing.screen)
        )
    }
}

@Composable
private fun AlarmCard(
    alarm: AlarmRule,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    AppCard(onClick = onClick, contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.lg)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatClock(alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.displayMedium,
                    color = if (alarm.enabled) AppTheme.colors.textPrimary else AppTheme.colors.textMuted
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = listOf(alarm.label, alarm.repeatSummary()).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (alarm.enabled) AppTheme.colors.textSecondary else AppTheme.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            AppSwitch(checked = alarm.enabled, onCheckedChange = onToggle)
        }
    }
}
