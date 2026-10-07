package com.example.screenmanager.ui.feature.limits.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.screenmanager.model.AppLimitRule
import com.example.screenmanager.model.AppOption
import com.example.screenmanager.ui.common.formatMinutes
import com.example.screenmanager.ui.components.AppTextField
import com.example.screenmanager.ui.components.SliderRow

/** Forma za dnevni limit (grupa aplikacija deli jedan dnevni budžet). */
@Composable
fun AppLimitEditor(
    initial: AppLimitRule,
    isNew: Boolean,
    installedApps: List<AppOption>,
    labelFor: (String) -> String,
    onSave: (AppLimitRule) -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit
) {
    var draft by rememberSaveable { mutableStateOf(initial) }

    RuleEditorScaffold(
        title = if (isNew) "New daily limit" else "Daily limit",
        onBack = onBack,
        canSave = draft.selectedAppIds.isNotEmpty(),
        onSave = { onSave(draft.copy(name = draft.name.trim().ifBlank { "Daily limit" })) },
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
            title = "Limit",
            hint = "Time in all selected apps is added together. Once the limit is used up, " +
                "they stay blocked until midnight."
        ) {
            SliderRow(
                label = "Time per day",
                value = draft.dailyLimitMinutes,
                onValueChange = { draft = draft.copy(dailyLimitMinutes = it) },
                range = 5..480,
                step = 5,
                valueLabel = ::formatMinutes
            )
        }
    }
}
