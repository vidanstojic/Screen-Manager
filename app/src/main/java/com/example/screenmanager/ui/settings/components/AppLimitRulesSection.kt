package com.example.screenmanager.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.screenmanager.model.AppLimitRule
import com.example.screenmanager.ui.components.SectionCard
import com.example.screenmanager.ui.components.StatusChip
import com.example.screenmanager.ui.theme.GlassTheme

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
        subtitle = "Daily cap for a group of apps (their usage is summed). Once it is used up, the whole group is blocked until midnight."
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Rules", color = GlassTheme.colors.textPrimary)
            FilledTonalIconButton(
                onClick = onAddRule,
                colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = GlassTheme.colors.surfaceElevated,
                    contentColor = GlassTheme.colors.textPrimary
                )
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add app limit")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        if (rules.isEmpty()) {
            Text("No app limits yet. Add one to start blocking by usage.", color = GlassTheme.colors.textSecondary)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rules.forEach { rule ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GlassTheme.colors.surfaceElevated),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(1.dp, GlassTheme.colors.borderEnd, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(rule.name, color = GlassTheme.colors.textPrimary)
                                    Text("${rule.dailyLimitMinutes}m daily cap • blocked until midnight", color = GlassTheme.colors.textSecondary)
                                }
                                Switch(
                                    checked = rule.isEnabled,
                                    onCheckedChange = { onToggleRule(rule.id, it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = GlassTheme.colors.textPrimary,
                                        checkedTrackColor = GlassTheme.colors.accentPrimary
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatusChip(text = "${rule.selectedAppIds.size} apps", isActive = rule.selectedAppIds.isNotEmpty())
                                StatusChip(text = if (rule.description.isBlank()) "No note" else "Note set", isActive = rule.description.isNotBlank())
                            }
                            if (rule.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(rule.description, color = GlassTheme.colors.textSecondary)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { onEditRule(rule) }) {
                                    Text("Edit", color = GlassTheme.colors.accentPrimary)
                                }
                                TextButton(onClick = { onRemoveRule(rule.id) }) {
                                    Text("Remove", color = GlassTheme.colors.accentSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = GlassTheme.colors.borderEnd)
        TextButton(onClick = onAddRule) {
            Text("Add app limit", color = GlassTheme.colors.accentPrimary)
        }
    }
}