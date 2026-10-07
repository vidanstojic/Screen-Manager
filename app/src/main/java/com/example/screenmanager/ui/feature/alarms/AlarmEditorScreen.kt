package com.example.screenmanager.ui.feature.alarms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.screenmanager.model.AlarmRule
import com.example.screenmanager.ui.components.AppCard
import com.example.screenmanager.ui.components.AppScreen
import com.example.screenmanager.ui.components.AppTextField
import com.example.screenmanager.ui.components.BottomActionBar
import com.example.screenmanager.ui.components.CircleIconButton
import com.example.screenmanager.ui.components.PrimaryButton
import com.example.screenmanager.ui.components.ScreenContentPadding
import com.example.screenmanager.ui.components.WeekdaySelector
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Stateful ulaz u formu alarma. `alarmId == null` → novi alarm sa trenutnim
 * vremenom; inače se učitava postojeći alarm.
 */
@Composable
fun AlarmEditorRoute(
    alarmId: Long?,
    onDone: () -> Unit,
    viewModel: AlarmsViewModel = viewModel()
) {
    val alarms by viewModel.alarms.collectAsState()
    val loaded = alarms
    if (loaded == null) {
        AppScreen(title = "", onBack = onDone) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppTheme.colors.accent)
            }
        }
        return
    }

    val existing = remember(alarmId) { loaded.firstOrNull { it.id == alarmId } }
    AlarmEditorScreen(
        existing = existing,
        onSave = { alarm ->
            viewModel.saveAlarm(alarm)
            onDone()
        },
        onDelete = existing?.let { alarm ->
            {
                viewModel.deleteAlarm(alarm.id)
                onDone()
            }
        },
        onBack = onDone
    )
}

/** Forma alarma: vreme (točkovi), dani ponavljanja i naziv. */
@Composable
fun AlarmEditorScreen(
    existing: AlarmRule?,
    onSave: (AlarmRule) -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit
) {
    val initialTime = remember { LocalTime.now() }
    var hour by rememberSaveable { mutableIntStateOf(existing?.hour ?: initialTime.hour) }
    var minute by rememberSaveable { mutableIntStateOf(existing?.minute ?: initialTime.minute) }
    var label by rememberSaveable { mutableStateOf(existing?.label.orEmpty()) }
    // Dani se čuvaju kao redni brojevi (1 = ponedeljak) da prežive rotaciju ekrana.
    var dayNumbers by rememberSaveable {
        mutableStateOf(existing?.repeatDays?.toDaysOfWeek().orEmpty().map { it.value })
    }
    val selectedDays = dayNumbers.map { DayOfWeek.of(it) }.toSet()

    AppScreen(
        title = if (existing == null) "New alarm" else "Edit alarm",
        onBack = onBack,
        actions = {
            if (onDelete != null) {
                CircleIconButton(
                    icon = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete alarm",
                    onClick = onDelete
                )
            }
        },
        bottomBar = {
            BottomActionBar {
                PrimaryButton(
                    text = "Save",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onSave(
                            AlarmRule(
                                id = existing?.id ?: System.currentTimeMillis(),
                                hour = hour,
                                minute = minute,
                                label = label.trim(),
                                repeatDays = selectedDays.toRepeatTokens(),
                                enabled = true
                            )
                        )
                    }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(ScreenContentPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            AppCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WheelPicker(count = 24, initialIndex = hour, onValueSelected = { hour = it })
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.displayMedium,
                        color = AppTheme.colors.textPrimary,
                        modifier = Modifier.padding(horizontal = Spacing.lg)
                    )
                    WheelPicker(count = 60, initialIndex = minute, onValueSelected = { minute = it })
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = "REPEAT",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.textMuted,
                    modifier = Modifier.padding(start = Spacing.xs)
                )
                AppCard {
                    WeekdaySelector(
                        selected = selectedDays,
                        onToggle = { day ->
                            dayNumbers = if (day in selectedDays) dayNumbers - day.value else dayNumbers + day.value
                        }
                    )
                }
                Text(
                    text = if (selectedDays.isEmpty()) {
                        "No days selected: the alarm rings once, the next time the clock shows this time."
                    } else {
                        "Rings on every selected day."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary,
                    modifier = Modifier.padding(horizontal = Spacing.xs)
                )
            }

            AppTextField(
                value = label,
                onValueChange = { label = it },
                label = "Name (optional)"
            )
        }
    }
}
