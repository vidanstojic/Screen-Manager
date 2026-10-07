package com.example.screenmanager.ui.feature.limits.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.screenmanager.model.AppOption
import com.example.screenmanager.model.WakeUpConfig
import com.example.screenmanager.ui.common.formatMinutes
import com.example.screenmanager.ui.components.CardDivider
import com.example.screenmanager.ui.components.StepperRow
import com.example.screenmanager.ui.components.SwitchRow

/** Forma za jutarnju blokadu (wake-up lockout, TRS 2.5). */
@Composable
fun MorningLockEditor(
    initial: WakeUpConfig,
    installedApps: List<AppOption>,
    labelFor: (String) -> String,
    onSave: (WakeUpConfig) -> Unit,
    onBack: () -> Unit
) {
    var draft by rememberSaveable { mutableStateOf(initial) }

    RuleEditorScaffold(
        title = "Morning lock",
        onBack = onBack,
        canSave = true,
        onSave = { onSave(draft) }
    ) {
        FormCard {
            SwitchRow(
                title = "Lock apps after waking up",
                checked = draft.isEnabled,
                onCheckedChange = { draft = draft.copy(isEnabled = it) }
            )
        }

        AppsFormCard(
            selectedIds = draft.selectedAppIds,
            onSelectionChange = { draft = draft.copy(selectedAppIds = it) },
            installedApps = installedApps,
            labelFor = labelFor
        )

        FormCard(
            title = "When",
            hint = "If the screen stays off for at least ${draft.inactivityHours}h (a night's sleep), " +
                "the selected apps are locked for ${formatMinutes(draft.blockDurationMinutes)} once " +
                "you start using the phone again."
        ) {
            StepperRow(
                label = "Phone unused for at least",
                value = draft.inactivityHours,
                onValueChange = { draft = draft.copy(inactivityHours = it) },
                range = 1..12,
                valueLabel = { "${it}h" }
            )
            CardDivider()
            StepperRow(
                label = "Keep apps locked for",
                value = draft.blockDurationMinutes,
                onValueChange = { draft = draft.copy(blockDurationMinutes = it) },
                range = 5..120,
                step = 5,
                valueLabel = ::formatMinutes
            )
        }
    }
}
