package com.example.screenmanager.ui.settings.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.screenmanager.model.ScheduleRule
import com.example.screenmanager.ui.components.SectionCard
import com.example.screenmanager.ui.components.StatusChip
import com.example.screenmanager.ui.theme.GlassTheme

@Composable
fun ScheduledBlockSection(
    rules: List<ScheduleRule>,
    onToggleRule: (String, Boolean) -> Unit,
    onAddScheduleClick: () -> Unit
) {
    SectionCard(
        title = "Scheduled blocks",
        subtitle = "Block selected apps during configured days and time ranges, with support for emergency sessions."
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Rules", color = GlassTheme.colors.textPrimary)
            FilledTonalIconButton(
                onClick = onAddScheduleClick,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = GlassTheme.colors.surfaceElevated,
                    contentColor = GlassTheme.colors.textPrimary
                )
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add schedule")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        if (rules.isEmpty()) {
            Text("No schedules yet. Add one to block apps automatically.", color = GlassTheme.colors.textSecondary)
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
                                    Text("${rule.startTime} - ${rule.endTime}", color = GlassTheme.colors.textSecondary)
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
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(rule.daysOfWeek.toList()) { day ->
                                    StatusChip(text = day.name.take(3), isActive = true)
                                }
                            }
                            if (rule.selectedAppIds.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Apps: ${rule.selectedAppIds.joinToString()}",
                                    color = GlassTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = GlassTheme.colors.borderEnd)
        TextButton(onClick = onAddScheduleClick) {
            Text("Add another schedule", color = GlassTheme.colors.accentPrimary)
        }
    }
}