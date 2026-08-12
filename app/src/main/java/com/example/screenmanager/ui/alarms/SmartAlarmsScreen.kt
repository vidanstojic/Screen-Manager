package com.example.screenmanager.ui.alarms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAlarmsScreen(
    onBack: () -> Unit
) {
    val alarms = remember {
        mutableStateListOf(
            AlarmItem(
                id = 1L,
                hour = 7,
                minute = 30,
                label = "Morning alarm",
                repeatDays = setOf("Mon", "Tue", "Wed", "Thu", "Fri"),
                enabled = true
            )
        )
    }
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddAlarmDialog(
            onDismiss = { showAddDialog = false },
            onSave = { alarm ->
                alarms.add(alarm)
                showAddDialog = false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B0A1F), Color(0xFF1A1233), Color(0xFF0E0B22))
                )
            )
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val maxWidth = this.maxWidth

            GlowOrb(
                colors = listOf(Color(0xFFB13BFF), Color(0xFF6C4CE0)),
                size = maxWidth * 0.5f,
                alignment = Alignment.TopEnd,
                offsetX = maxWidth * 0.1f,
                offsetY = (-50).dp
            )
            GlowOrb(
                colors = listOf(Color(0xFF3BC8FF), Color(0xFF4C6CE0)),
                size = maxWidth * 0.35f,
                alignment = Alignment.CenterStart,
                offsetX = -(maxWidth * 0.15f),
                offsetY = 60.dp
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color(0xFFFF5C8A),
                    contentColor = Color.White,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add alarm")
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Column {
                            Text(
                                text = "Smart Alarms",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Regular alarms with quick repeat setup",
                                color = Color(0xFFA9A3C4),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                item {
                    GlassInfoCard("Dodaj obične alarme, ponavljanje po danima i uključi/isključi ih.")
                }

                if (alarms.isEmpty()) {
                    item {
                        GlassInfoCard("Nema alarma. Klikni + da dodaš prvi alarm.")
                    }
                } else {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmRow(
                            alarm = alarm,
                            onToggle = { enabled ->
                                val index = alarms.indexOfFirst { it.id == alarm.id }
                                if (index >= 0) alarms[index] = alarms[index].copy(enabled = enabled)
                            },
                            onDelete = {
                                alarms.removeAll { it.id == alarm.id }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: AlarmItem,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(
                1.dp,
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.06f))),
                RoundedCornerShape(20.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatTime(alarm.hour, alarm.minute),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = alarm.label.ifBlank { "Alarm" },
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFEDE9FF)
            )
            Spacer(Modifier.size(2.dp))
            Text(
                text = alarm.repeatDays.sorted().joinToString(", ").ifBlank { "One-time" },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFA9A3C4)
            )
        }
        Switch(
            checked = alarm.enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFB13BFF),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF6C647F)
            )
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete alarm", tint = Color(0xFFFF8FB1))
        }
    }
}

@Composable
private fun GlassInfoCard(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(
                1.dp,
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.06f))),
                RoundedCornerShape(20.dp)
            )
            .padding(14.dp)
    ) {
        Text(
            text = text,
            color = Color(0xFFD6D0EE),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun BoxScope.GlowOrb(
    colors: List<Color>,
    size: Dp,
    alignment: Alignment,
    offsetX: Dp,
    offsetY: Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .align(alignment)
            .offset(x = offsetX, y = offsetY)
            .blur(90.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(colors.map { it.copy(alpha = 0.45f) })
            )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepeatChipsRow(
    days: List<String>,
    selectedDays: Set<String>,
    onToggleDay: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        days.forEach { day ->
            FilterChip(
                selected = selectedDays.contains(day),
                onClick = { onToggleDay(day) },
                label = { Text(day) }
            )
        }
    }
}

@Composable
private fun AddAlarmDialog(
    onDismiss: () -> Unit,
    onSave: (AlarmItem) -> Unit
) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    var hour by remember { mutableStateOf(7f) }
    var minute by remember { mutableStateOf(30f) }
    var label by remember { mutableStateOf("") }
    var enabled by remember { mutableStateOf(true) }
    var selectedDays by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1233),
        title = { Text("Add regular alarm", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                )

                Text("Time: ${formatTime(hour.toInt(), minute.toInt())}", color = Color.White)
                Text("Hour: ${hour.toInt()}", color = Color(0xFFD6D0EE))
                Slider(
                    value = hour,
                    onValueChange = { hour = it },
                    valueRange = 0f..23f,
                    steps = 22
                )
                Text("Minute: ${minute.toInt()}", color = Color(0xFFD6D0EE))
                Slider(
                    value = minute,
                    onValueChange = { minute = it },
                    valueRange = 0f..59f,
                    steps = 58
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enabled", modifier = Modifier.weight(1f), color = Color.White)
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }

                Text("Repeat", color = Color.White)
                RepeatChipsRow(
                    days = days.take(4),
                    selectedDays = selectedDays,
                    onToggleDay = { day ->
                        selectedDays = if (selectedDays.contains(day)) {
                            selectedDays - day
                        } else {
                            selectedDays + day
                        }
                    }
                )
                RepeatChipsRow(
                    days = days.drop(4),
                    selectedDays = selectedDays,
                    onToggleDay = { day ->
                        selectedDays = if (selectedDays.contains(day)) {
                            selectedDays - day
                        } else {
                            selectedDays + day
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        AlarmItem(
                            id = System.currentTimeMillis(),
                            hour = hour.toInt(),
                            minute = minute.toInt(),
                            label = label.trim(),
                            repeatDays = selectedDays,
                            enabled = enabled
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTime(hour: Int, minute: Int): String {
    return String.format("%02d:%02d", hour, minute)
}

private data class AlarmItem(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val label: String,
    val repeatDays: Set<String>,
    val enabled: Boolean
)
