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
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.screenmanager.ui.components.SecondaryButton
import com.example.screenmanager.ui.components.WeekdaySelector
import com.example.screenmanager.ui.components.screenContentPadding
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
        onTest = viewModel::testAlarm,
        onBack = onDone
    )
}

/**
 * Forma alarma. Radi nad radnom kopijom ([AlarmRule] je Serializable, pa
 * preživljava rotaciju); u bazu se upisuje tek na Save. "Test" pušta alarm
 * odmah, sa podešavanjima iz forme.
 */
@Composable
fun AlarmEditorScreen(
    existing: AlarmRule?,
    onSave: (AlarmRule) -> Unit,
    onDelete: (() -> Unit)?,
    onTest: (AlarmRule) -> Unit,
    onBack: () -> Unit
) {
    var draft by rememberSaveable {
        val now = LocalTime.now()
        mutableStateOf(
            existing ?: AlarmRule(
                id = System.currentTimeMillis(),
                hour = now.hour,
                minute = now.minute,
                label = "",
                repeatDays = emptySet(),
                enabled = true
            )
        )
    }
    val finalDraft = { draft.copy(label = draft.label.trim(), enabled = true) }

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
                SecondaryButton(
                    text = "Test",
                    icon = Icons.Rounded.PlayArrow,
                    onClick = { onTest(finalDraft()) }
                )
                PrimaryButton(
                    text = "Save",
                    modifier = Modifier.weight(1f),
                    onClick = { onSave(finalDraft()) }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(screenContentPadding()),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            AlarmTimeCard(
                hour = draft.hour,
                minute = draft.minute,
                onTimeChange = { hour, minute -> draft = draft.copy(hour = hour, minute = minute) }
            )
            RepeatSection(
                days = draft.repeatDays.toDaysOfWeek(),
                onDaysChange = { days -> draft = draft.copy(repeatDays = days.toRepeatTokens()) }
            )
            AppTextField(
                value = draft.label,
                onValueChange = { draft = draft.copy(label = it) },
                label = "Name (optional)"
            )
            AlarmSoundSection(sound = draft.sound, onChange = { draft = draft.copy(sound = it) })
            AlarmSnoozeSection(snooze = draft.snooze, onChange = { draft = draft.copy(snooze = it) })
            AlarmMissionSection(mission = draft.mission, onChange = { draft = draft.copy(mission = it) })
        }
    }
}

@Composable
private fun AlarmTimeCard(hour: Int, minute: Int, onTimeChange: (hour: Int, minute: Int) -> Unit) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelPicker(count = 24, initialIndex = hour, onValueSelected = { onTimeChange(it, minute) })
            Text(
                text = ":",
                style = MaterialTheme.typography.displayMedium,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.padding(horizontal = Spacing.lg)
            )
            WheelPicker(count = 60, initialIndex = minute, onValueSelected = { onTimeChange(hour, it) })
        }
    }
}

@Composable
private fun RepeatSection(days: Set<DayOfWeek>, onDaysChange: (Set<DayOfWeek>) -> Unit) {
    EditorSection(title = "Repeat") {
        AppCard {
            WeekdaySelector(
                selected = days,
                onToggle = { day -> onDaysChange(if (day in days) days - day else days + day) }
            )
        }
        EditorHint(
            if (days.isEmpty()) {
                "No days selected: the alarm rings once, the next time the clock shows this time."
            } else {
                "Rings on every selected day."
            }
        )
    }
}
