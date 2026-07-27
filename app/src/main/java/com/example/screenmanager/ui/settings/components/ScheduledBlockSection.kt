package com.example.screenmanager.ui.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.screenmanager.model.ScheduleRule

@Composable
fun ScheduledBlockSection(
    rules: List<ScheduleRule>,
    onToggleRule: (String, Boolean) -> Unit,
    onAddScheduleClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Zakazana blokiranja", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            rules.forEach { rule ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = rule.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "${rule.startTime} - ${rule.endTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = rule.isEnabled,
                        onCheckedChange = { isChecked -> onToggleRule(rule.id, isChecked) }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onAddScheduleClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Dodaj novi raspored")
            }
        }
    }
}