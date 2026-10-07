package com.example.screenmanager.ui.feature.limits.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.screenmanager.model.AppOption
import com.example.screenmanager.model.SessionLimitRule
import com.example.screenmanager.ui.common.formatMinutes
import com.example.screenmanager.ui.components.AppTextField
import com.example.screenmanager.ui.components.CardDivider
import com.example.screenmanager.ui.components.StepperRow

/** Forma za interval pravilo: M minuta po sesiji, N sesija dnevno, K minuta pauze. */
@Composable
fun SessionLimitEditor(
    initial: SessionLimitRule,
    isNew: Boolean,
    installedApps: List<AppOption>,
    labelFor: (String) -> String,
    onSave: (SessionLimitRule) -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit
) {
    var draft by rememberSaveable { mutableStateOf(initial) }

    RuleEditorScaffold(
        title = if (isNew) "New session limit" else "Session limit",
        onBack = onBack,
        canSave = draft.selectedAppIds.isNotEmpty(),
        onSave = { onSave(draft.copy(name = draft.name.trim().ifBlank { "Session limit" })) },
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
            title = "Sessions",
            hint = "When a session runs out, the apps are locked for the break. After the last " +
                "session of the day they stay locked until midnight."
        ) {
            SessionSteppers(
                sessionLengthMinutes = draft.sessionLengthMinutes,
                maxSessions = draft.maxSessions,
                cooldownMinutes = draft.cooldownMinutes,
                onSessionLengthChange = { draft = draft.copy(sessionLengthMinutes = it) },
                onMaxSessionsChange = { draft = draft.copy(maxSessions = it) },
                onCooldownChange = { draft = draft.copy(cooldownMinutes = it) }
            )
        }
    }
}

/**
 * Tri polja interval moda. Deli ih forma za aplikacije i forma za
 * Shorts/Reels (mod "Sessions").
 */
@Composable
fun SessionSteppers(
    sessionLengthMinutes: Int,
    maxSessions: Int,
    cooldownMinutes: Int,
    onSessionLengthChange: (Int) -> Unit,
    onMaxSessionsChange: (Int) -> Unit,
    onCooldownChange: (Int) -> Unit
) {
    StepperRow(
        label = "Session length",
        value = sessionLengthMinutes,
        onValueChange = onSessionLengthChange,
        range = 1..120,
        valueLabel = ::formatMinutes
    )
    CardDivider()
    StepperRow(
        label = "Sessions per day",
        value = maxSessions,
        onValueChange = onMaxSessionsChange,
        range = 1..50
    )
    CardDivider()
    StepperRow(
        label = "Break after a session",
        value = cooldownMinutes,
        onValueChange = onCooldownChange,
        range = 0..240,
        step = 5,
        valueLabel = { if (it == 0) "None" else formatMinutes(it) }
    )
}
