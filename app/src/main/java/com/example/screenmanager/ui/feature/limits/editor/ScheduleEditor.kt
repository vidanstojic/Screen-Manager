package com.example.screenmanager.ui.feature.limits.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.screenmanager.model.AppOption
import com.example.screenmanager.model.ScheduleRule
import com.example.screenmanager.ui.components.AppTextField
import com.example.screenmanager.ui.components.AppTimePickerDialog
import com.example.screenmanager.ui.components.TimeField
import com.example.screenmanager.ui.components.WeekdaySelector
import com.example.screenmanager.ui.theme.Spacing

private enum class TimeSlot { Start, End }

/** Forma za zakazanu blokadu: vremenski prozor + dani u nedelji. */
@Composable
fun ScheduleEditor(
    initial: ScheduleRule,
    isNew: Boolean,
    installedApps: List<AppOption>,
    labelFor: (String) -> String,
    onSave: (ScheduleRule) -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit
) {
    var draft by rememberSaveable { mutableStateOf(initial) }
    var editingTime by rememberSaveable { mutableStateOf<TimeSlot?>(null) }

    RuleEditorScaffold(
        title = if (isNew) "New schedule" else "Schedule",
        onBack = onBack,
        canSave = draft.selectedAppIds.isNotEmpty() && draft.daysOfWeek.isNotEmpty(),
        onSave = { onSave(draft.copy(name = draft.name.trim().ifBlank { "Schedule" })) },
        onDelete = onDelete
    ) {
        AppTextField(
            value = draft.name,
            onValueChange = { draft = draft.copy(name = it) },
            label = "Name"
        )

        AppsFormCard(
            selectedIds = draft.selectedAppIds,
            onSelectionChange = { draft = draft.copy(selectedAppIds = it) },
            installedApps = installedApps,
            labelFor = labelFor
        )

        FormCard(
            title = "Blocked hours",
            hint = if (draft.endTime <= draft.startTime) {
                "Ends the next day. An overnight block belongs to the day it starts on."
            } else {
                null
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                TimeField(
                    label = "From",
                    time = draft.startTime,
                    onClick = { editingTime = TimeSlot.Start },
                    modifier = Modifier.weight(1f)
                )
                TimeField(
                    label = "To",
                    time = draft.endTime,
                    onClick = { editingTime = TimeSlot.End },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        FormCard(title = "Days") {
            WeekdaySelector(
                selected = draft.daysOfWeek,
                onToggle = { day ->
                    val days = if (day in draft.daysOfWeek) draft.daysOfWeek - day else draft.daysOfWeek + day
                    draft = draft.copy(daysOfWeek = days)
                },
                modifier = Modifier.padding(Spacing.lg)
            )
        }
    }

    when (editingTime) {
        TimeSlot.Start -> AppTimePickerDialog(
            title = "Block from",
            initialTime = draft.startTime,
            onDismiss = { editingTime = null },
            onConfirm = {
                draft = draft.copy(startTime = it)
                editingTime = null
            }
        )

        TimeSlot.End -> AppTimePickerDialog(
            title = "Block until",
            initialTime = draft.endTime,
            onDismiss = { editingTime = null },
            onConfirm = {
                draft = draft.copy(endTime = it)
                editingTime = null
            }
        )

        null -> Unit
    }
}
