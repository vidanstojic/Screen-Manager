package com.example.screenmanager.ui.limits

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.screenmanager.domain.getInstalledApps
import com.example.screenmanager.model.AppOption
import com.example.screenmanager.model.ScheduleRule
import com.example.screenmanager.ui.settings.components.AppPickerDialog
import com.example.screenmanager.ui.theme.PurpleAccent
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

@Composable
fun AddScheduleDialog(
    availableApps: List<AppOption>,
    onDismiss: () -> Unit,
    onSaveRule: (ScheduleRule) -> Unit
) {
    val context = LocalContext.current

    var ruleName by remember { mutableStateOf("") }
    var startHour by remember { mutableStateOf(9) }
    var startMinute by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(17) }
    var endMinute by remember { mutableStateOf(0) }

    var selectedDays by remember { mutableStateOf(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) }
    var selectedAppIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var showAppPicker by remember { mutableStateOf(false) }

    // TimePicker dijalog za početno i krajnje vreme
    fun pickTime(initialHour: Int, initialMinute: Int, onTimePicked: (Int, Int) -> Unit) {
        TimePickerDialog(
            context,
            { _, h, m -> onTimePicked(h, m) },
            initialHour,
            initialMinute,
            true
        ).show()
    }

    if (showAppPicker) {
        AppPickerDialog(
            availableApps = availableApps,
            initialSelectedIds = selectedAppIds,
            onDismiss = { showAppPicker = false },
            onConfirm = { ids ->
                selectedAppIds = ids
                showAppPicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Scheduled Block", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Naziv pravila
                OutlinedTextField(
                    value = ruleName,
                    onValueChange = { ruleName = it },
                    label = { Text("Rule Name (e.g. Work Focus)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Izbor Vremena (Od - Do)
                Text("Time Interval", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimeBox(
                        label = "From",
                        timeStr = String.format("%02d:%02d", startHour, startMinute),
                        onClick = { pickTime(startHour, startMinute) { h, m -> startHour = h; startMinute = m } }
                    )
                    Text("-", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    TimeBox(
                        label = "To",
                        timeStr = String.format("%02d:%02d", endHour, endMinute),
                        onClick = { pickTime(endHour, endMinute) { h, m -> endHour = h; endMinute = m } }
                    )
                }

                // Izbor Dana u nedelji
                Text("Days of Week", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val daysMap = mapOf(
                        DayOfWeek.MONDAY to "M",
                        DayOfWeek.TUESDAY to "T",
                        DayOfWeek.WEDNESDAY to "W",
                        DayOfWeek.THURSDAY to "T",
                        DayOfWeek.FRIDAY to "F",
                        DayOfWeek.SATURDAY to "S",
                        DayOfWeek.SUNDAY to "S"
                    )

                    daysMap.forEach { (day, label) ->
                        val isSelected = selectedDays.contains(day)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) PurpleAccent else Color(0xFFF0F0F0))
                                .clickable {
                                    selectedDays = if (isSelected) selectedDays - day else selectedDays + day
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Izbor Aplikacija
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Blocked Apps", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            text = if (selectedAppIds.isEmpty()) "No apps selected" else "${selectedAppIds.size} apps selected",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Button(
                        onClick = { showAppPicker = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                    ) {
                        Text("Select Apps")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newRule = ScheduleRule(
                        id = UUID.randomUUID().toString(),
                        name = if (ruleName.isBlank()) "Scheduled Block" else ruleName,
                        startTime = LocalTime.of(startHour, startMinute),
                        endTime = LocalTime.of(endHour, endMinute),
                        daysOfWeek = selectedDays,
                        selectedAppIds = selectedAppIds,
                        isEnabled = true
                    )
                    onSaveRule(newRule)
                },
                enabled = selectedAppIds.isNotEmpty() && selectedDays.isNotEmpty()
            ) {
                Text("Save Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TimeBox(label: String, timeStr: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(timeStr, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}