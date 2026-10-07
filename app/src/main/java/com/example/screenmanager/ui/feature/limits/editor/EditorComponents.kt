package com.example.screenmanager.ui.feature.limits.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.screenmanager.model.AppOption
import com.example.screenmanager.ui.components.AppCard
import com.example.screenmanager.ui.components.AppPickerDialog
import com.example.screenmanager.ui.components.AppScreen
import com.example.screenmanager.ui.components.AppSelectionField
import com.example.screenmanager.ui.components.BottomActionBar
import com.example.screenmanager.ui.components.CircleIconButton
import com.example.screenmanager.ui.components.PrimaryButton
import com.example.screenmanager.ui.components.ScreenContentPadding
import com.example.screenmanager.ui.components.TextAction
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing

/**
 * Zajednički kostur svih formi za pravila: naslov sa strelicom nazad,
 * (opciono) brisanje uz potvrdu, skrolujući sadržaj i "Save" pri dnu.
 */
@Composable
fun RuleEditorScaffold(
    title: String,
    onBack: () -> Unit,
    canSave: Boolean,
    onSave: () -> Unit,
    onDelete: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }

    AppScreen(
        title = title,
        onBack = onBack,
        actions = {
            if (onDelete != null) {
                CircleIconButton(
                    icon = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete rule",
                    onClick = { confirmDelete = true }
                )
            }
        },
        bottomBar = {
            BottomActionBar {
                PrimaryButton(text = "Save", onClick = onSave, enabled = canSave, modifier = Modifier.weight(1f))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(ScreenContentPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            content = content
        )
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = AppTheme.colors.surface,
            title = { Text("Delete this rule?", color = AppTheme.colors.textPrimary) },
            text = { Text("The apps it covers will no longer be limited by it.", color = AppTheme.colors.textSecondary) },
            confirmButton = {
                TextAction(
                    text = "Delete",
                    color = AppTheme.colors.danger,
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    }
                )
            },
            dismissButton = {
                TextAction(text = "Cancel", color = AppTheme.colors.textSecondary, onClick = { confirmDelete = false })
            }
        )
    }
}

/** Grupa polja forme: mali naslov iznad kartice, polja kao redovi unutar nje. */
@Composable
fun FormCard(
    title: String? = null,
    hint: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (title != null) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = AppTheme.colors.textMuted,
                modifier = Modifier.padding(start = Spacing.xs)
            )
        }
        AppCard(contentPadding = PaddingValues(0.dp), content = content)
        if (hint != null) FormHint(hint)
    }
}

/** Objašnjenje ispod grupe polja. */
@Composable
fun FormHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = AppTheme.colors.textSecondary,
        modifier = Modifier.padding(horizontal = Spacing.xs)
    )
}

/** Grupa "Apps": prikazuje izabrane aplikacije i sama otvara izbor aplikacija. */
@Composable
fun AppsFormCard(
    selectedIds: List<String>,
    onSelectionChange: (List<String>) -> Unit,
    installedApps: List<AppOption>,
    labelFor: (String) -> String
) {
    var showPicker by remember { mutableStateOf(false) }

    FormCard(title = "Apps") {
        AppSelectionField(
            selectedIds = selectedIds,
            labelFor = labelFor,
            onClick = { showPicker = true }
        )
    }

    if (showPicker) {
        AppPickerDialog(
            availableApps = installedApps,
            initialSelectedIds = selectedIds,
            onDismiss = { showPicker = false },
            onConfirm = { ids ->
                onSelectionChange(ids)
                showPicker = false
            }
        )
    }
}
