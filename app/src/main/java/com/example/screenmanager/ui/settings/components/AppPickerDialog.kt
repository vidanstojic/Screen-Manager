package com.example.screenmanager.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.screenmanager.model.AppOption
import com.example.screenmanager.ui.theme.GlassTheme

@Composable
fun AppPickerDialog(
    availableApps: List<AppOption>,
    initialSelectedIds: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(initialSelectedIds.toSet()) }

    val filteredApps = remember(query, availableApps) {
        if (query.isBlank()) {
            availableApps
        } else {
            availableApps.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.category.contains(query, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassTheme.colors.surface,
        shape = RoundedCornerShape(18.dp),
        title = { Text(text = "Choose apps", fontWeight = FontWeight.Bold, color = GlassTheme.colors.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search apps", color = GlassTheme.colors.textSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GlassTheme.colors.accentPrimary,
                        unfocusedBorderColor = GlassTheme.colors.borderEnd,
                        focusedLabelColor = GlassTheme.colors.accentPrimary,
                        unfocusedLabelColor = GlassTheme.colors.textSecondary,
                        focusedTextColor = GlassTheme.colors.textPrimary,
                        unfocusedTextColor = GlassTheme.colors.textPrimary
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { query = "" },
                        label = { Text("All", color = GlassTheme.colors.textPrimary) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = GlassTheme.colors.surfaceElevated)
                    )
                    AssistChip(
                        onClick = { query = "YouTube" },
                        label = { Text("Video", color = GlassTheme.colors.textPrimary) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = GlassTheme.colors.surfaceElevated)
                    )
                    AssistChip(
                        onClick = { query = "Instagram" },
                        label = { Text("Social", color = GlassTheme.colors.textPrimary) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = GlassTheme.colors.surfaceElevated)
                    )
                }

                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredApps, key = { it.id }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIds = if (selectedIds.contains(app.id)) {
                                        selectedIds - app.id
                                    } else {
                                        selectedIds + app.id
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(38.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Checkbox(
                                    checked = selectedIds.contains(app.id),
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = GlassTheme.colors.accentPrimary,
                                        uncheckedColor = GlassTheme.colors.textSecondary
                                    )
                                )
                            }
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(app.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = GlassTheme.colors.textPrimary)
                                Text(app.category, style = MaterialTheme.typography.bodySmall, color = GlassTheme.colors.textSecondary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedIds.toList()) }) {
                Text("Save", color = GlassTheme.colors.accentPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = GlassTheme.colors.textSecondary)
            }
        }
    )
}