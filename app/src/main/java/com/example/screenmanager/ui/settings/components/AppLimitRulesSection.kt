package com.example.screenmanager.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.screenmanager.model.AppLimitRule
import com.example.screenmanager.ui.components.SectionCard
import com.example.screenmanager.ui.components.StatusChip

@Composable
fun AppLimitRulesSection(
    rules: List<AppLimitRule>,
    onToggleRule: (String, Boolean) -> Unit,
    onAddRule: () -> Unit,
    onEditRule: (AppLimitRule) -> Unit,
    onRemoveRule: (String) -> Unit
) {
    SectionCard(
        title = "App limits",
        subtitle = "Set a daily limit for any app, then block it for the same duration once the limit expires."
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Rules")
            FilledTonalIconButton(onClick = onAddRule) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add app limit")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        if (rules.isEmpty()) {
            Text("No app limits yet. Add one to start blocking by usage.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rules.forEach { rule ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F8FB))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(rule.name)
                                    Text("${rule.dailyLimitMinutes}m limit • ${rule.blockDurationMinutes}m block")
                                }
                                Switch(
                                    checked = rule.isEnabled,
                                    onCheckedChange = { onToggleRule(rule.id, it) }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatusChip(text = "${rule.selectedAppIds.size} apps", isActive = rule.selectedAppIds.isNotEmpty())
                                StatusChip(text = if (rule.description.isBlank()) "No note" else "Note set", isActive = rule.description.isNotBlank())
                            }
                            if (rule.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(rule.description)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { onEditRule(rule) }) { Text("Edit") }
                                TextButton(onClick = { onRemoveRule(rule.id) }) { Text("Remove") }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        TextButton(onClick = onAddRule) {
            Text("Add app limit")
        }
    }
}
