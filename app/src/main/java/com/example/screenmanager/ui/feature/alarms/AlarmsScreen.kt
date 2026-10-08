package com.example.screenmanager.ui.feature.alarms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.screenmanager.model.AlarmRule
import com.example.screenmanager.ui.common.formatClock
import com.example.screenmanager.ui.common.relativeDayLabel
import com.example.screenmanager.ui.common.rememberNow
import com.example.screenmanager.ui.components.AddFab
import com.example.screenmanager.ui.components.AppCard
import com.example.screenmanager.ui.components.AppScreen
import com.example.screenmanager.ui.components.AppSwitch
import com.example.screenmanager.ui.components.EmptyState
import com.example.screenmanager.ui.components.FabSpacer
import com.example.screenmanager.ui.components.screenContentPadding
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId

/** Stateful ulaz u Smart Alarms (zaseban deo aplikacije, bez donje navigacije). */
@Composable
fun AlarmsRoute(
    onBack: () -> Unit,
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
        onBack = onBack,
        onToggle = viewModel::setAlarmEnabled,
        onEditAlarm = onEditAlarm
    )
}

/** Lista alarma; klik otvara formu, "+" dodaje novi. Strelica nazad vraća na početni ekran. */
@Composable
fun AlarmsScreen(
    alarms: List<AlarmRule>,
    isLoading: Boolean,
    nextAlarmAt: Long?,
    now: Long,
    onBack: () -> Unit,
    onToggle: (AlarmRule, Boolean) -> Unit,
    onEditAlarm: (alarmId: Long?) -> Unit
) {
    AppScreen(title = "Smart Alarms", onBack = onBack, largeTitle = true) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = screenContentPadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            if (alarms.isNotEmpty()) {
                item(key = "next") { NextAlarmHero(nextAlarmAt = nextAlarmAt, now = now) }
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

/** Veliki sat sledećeg alarma, u bojama doba dana u kome zvoni. */
@Composable
private fun NextAlarmHero(nextAlarmAt: Long?, now: Long) {
    val colors = AppTheme.colors
    if (nextAlarmAt == null) {
        AppCard(shape = MaterialTheme.shapes.extraLarge, contentPadding = PaddingValues(Spacing.xl)) {
            Text(text = "NEXT ALARM", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
            Spacer(Modifier.height(Spacing.xs))
            Text(text = "All alarms are off", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
        }
        return
    }
    val time = Instant.ofEpochMilli(nextAlarmAt).atZone(ZoneId.systemDefault()).toLocalTime()
    val dayColors = timeOfDayColors(time.hour)
    AppCard(
        shape = MaterialTheme.shapes.extraLarge,
        tint = Brush.linearGradient(listOf(dayColors.last().copy(alpha = 0.40f), dayColors.first().copy(alpha = 0.08f))),
        contentPadding = PaddingValues(Spacing.xl)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "NEXT ALARM", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, modifier = Modifier.weight(1f))
            Icon(imageVector = timeOfDayIcon(time.hour), contentDescription = null, tint = dayColors.first())
        }
        Text(
            text = formatClock(time),
            style = MaterialTheme.typography.displayLarge.copy(brush = Brush.linearGradient(listOf(Color.White, dayColors.first())))
        )
        Text(
            text = "${relativeDayLabel(nextAlarmAt, now)} · ${formatTimeUntil(nextAlarmAt - now)}",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textSecondary
        )
    }
}

/** Kartica alarma: boja i ikonica prate doba dana; isključen alarm je prigušen. */
@Composable
private fun AlarmCard(
    alarm: AlarmRule,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val dayColors = timeOfDayColors(alarm.hour)
    AppCard(
        onClick = onClick,
        tint = if (alarm.enabled) {
            Brush.linearGradient(listOf(dayColors.last().copy(alpha = 0.22f), Color.Transparent))
        } else {
            null
        },
        contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatClock(alarm.hour, alarm.minute),
                    style = if (alarm.enabled) {
                        MaterialTheme.typography.displayMedium.copy(
                            brush = Brush.linearGradient(listOf(Color.White, dayColors.first()))
                        )
                    } else {
                        MaterialTheme.typography.displayMedium.copy(color = colors.textMuted)
                    }
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = timeOfDayIcon(alarm.hour),
                        contentDescription = null,
                        tint = if (alarm.enabled) dayColors.first() else colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = listOf(alarm.label, alarm.repeatSummary()).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (alarm.enabled) colors.textSecondary else colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            AppSwitch(checked = alarm.enabled, onCheckedChange = onToggle)
        }
    }
}
