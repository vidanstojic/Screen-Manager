package com.example.screenmanager.ui.alarms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.screenmanager.model.AlarmRule
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAlarmsScreen(
    viewModel: SmartAlarmsViewModel = viewModel(),
    onBack: () -> Unit
) {
    val alarms by viewModel.alarms.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }
    var alarmToEdit by remember { mutableStateOf<AlarmRule?>(null) }

    if (showBottomSheet) {
        AddAlarmBottomSheet(
            initialAlarm = alarmToEdit,
            onDismiss = {
                showBottomSheet = false
                alarmToEdit = null
            },
            onSave = { alarm ->
                viewModel.saveAlarm(alarm)
                showBottomSheet = false
                alarmToEdit = null
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
                    onClick = {
                        alarmToEdit = null
                        showBottomSheet = true
                    },
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
                    val headerText = remember(alarms) { getNextAlarmFormattedText(alarms) }
                    Text(
                        text = headerText,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, bottom = 16.dp)
                    )
                }

                if (alarms.isEmpty()) {
                    item {
                        GlassInfoCard("No alarms. Tap + to add your first alarm.")
                    }
                } else {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmRow(
                            alarm = alarm,
                            onToggle = { enabled -> viewModel.toggleAlarm(alarm.id, enabled) },
                            onClick = {
                                alarmToEdit = alarm
                                showBottomSheet = true
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
    alarm: AlarmRule,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatTime(alarm.hour, alarm.minute),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Light,
                    color = if (alarm.enabled) Color.White else Color.White.copy(alpha = 0.4f)
                )
            }
            Spacer(Modifier.size(4.dp))
            Text(
                text = buildString {
                    if (alarm.label.isNotBlank()) append("${alarm.label} • ")
                    append(alarm.repeatDays.sorted().joinToString(", ").ifBlank { "One-time" })
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (alarm.enabled) Color(0xFFA9A3C4) else Color(0xFFA9A3C4).copy(alpha = 0.4f)
            )
        }
        Switch(
            checked = alarm.enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFB13BFF),
                uncheckedThumbColor = Color(0xFF6C647F),
                uncheckedTrackColor = Color(0xFF1A1233)
            )
        )
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
            .padding(16.dp)
    ) {
        Text(
            text = text,
            color = Color(0xFFD6D0EE),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
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
private fun AddAlarmBottomSheet(
    initialAlarm: AlarmRule?,
    onDismiss: () -> Unit,
    onSave: (AlarmRule) -> Unit
) {
    val daysInitials = listOf("M", "T", "W", "T", "F", "S", "S")
    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    val calendar = Calendar.getInstance()
    var hour by remember { mutableIntStateOf(initialAlarm?.hour ?: calendar.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(initialAlarm?.minute ?: calendar.get(Calendar.MINUTE)) }
    var label by remember { mutableStateOf(initialAlarm?.label ?: "") }
    var selectedDays by remember { mutableStateOf(initialAlarm?.repeatDays ?: setOf<String>()) }

    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var snoozeEnabled by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0A1F))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(24.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Točkovi za vreme
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WheelPicker(
                        count = 24,
                        initialIndex = hour,
                        onScrollFinished = { hour = it }
                    )
                    Text(":", fontSize = 48.sp, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp))
                    WheelPicker(
                        count = 60,
                        initialIndex = minute,
                        onScrollFinished = { minute = it },
                        format2Digits = true
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Kružni dani
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    daysInitials.forEachIndexed { index, initial ->
                        val dayName = dayNames[index]
                        val isSelected = selectedDays.contains(dayName)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFFB13BFF) else Color(0xFF1A1233))
                                .border(1.dp, if (isSelected) Color.Transparent else Color.White.copy(0.1f), CircleShape)
                                .clickable {
                                    selectedDays = if (isSelected) selectedDays - dayName else selectedDays + dayName
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial,
                                color = if (isSelected) Color.White else if (index > 4) Color(0xFFFF5C8A) else Color(0xFFA9A3C4),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Alarm name", color = Color(0xFFA9A3C4)) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                )

                Spacer(modifier = Modifier.height(16.dp))

                SettingRowToggle("Sound", "Homecoming", soundEnabled) { soundEnabled = it }
                SettingRowToggle("Vibration", "Basic call", vibrationEnabled) { vibrationEnabled = it }
                SettingRowToggle("Snooze", "5 minutes, 3 times", snoozeEnabled) { snoozeEnabled = it }

                Spacer(modifier = Modifier.weight(1f))

                // Odvojena Cancel i Save dugmad po uzoru na Android alarm sa dodatim donjim padding-om da ne budu slepljeni
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF221A3B))
                    ) {
                        Text(
                            "Cancel",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    TextButton(
                        onClick = {
                            onSave(
                                AlarmRule(
                                    id = initialAlarm?.id ?: System.currentTimeMillis(),
                                    hour = hour,
                                    minute = minute,
                                    label = label.trim(),
                                    repeatDays = selectedDays,
                                    enabled = true
                                )
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFB13BFF))
                    ) {
                        Text(
                            "Save",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SettingRowToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(title, color = Color.White, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Color(0xFF6C4CE0), fontSize = 13.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFB13BFF),
                uncheckedThumbColor = Color(0xFFA9A3C4),
                uncheckedTrackColor = Color(0xFF1A1233)
            )
        )
    }
}

@Composable
fun WheelPicker(
    count: Int,
    initialIndex: Int,
    onScrollFinished: (Int) -> Unit,
    format2Digits: Boolean = false
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerItem = listState.firstVisibleItemIndex
            if (centerItem < count) {
                onScrollFinished(centerItem)
            }
        }
    }

    Box(
        modifier = Modifier
            .width(80.dp)
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 65.dp)
        ) {
            items(count) { index ->
                val text = if (format2Digits) String.format("%02d", index) else index.toString()
                val isCenter = index == listState.firstVisibleItemIndex

                Text(
                    text = text,
                    fontSize = if (isCenter) 48.sp else 32.sp,
                    color = if (isCenter) Color.White else Color.White.copy(alpha = 0.2f),
                    fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                )
            }
        }
    }
}

private fun getNextAlarmFormattedText(alarms: List<AlarmRule>): String {
    val enabledAlarms = alarms.filter { it.enabled }
    if (enabledAlarms.isEmpty()) {
        return "All alarms are off"
    }

    val now = Calendar.getInstance()
    val nowMillis = now.timeInMillis
    var minDiff = Long.MAX_VALUE

    for (alarm in enabledAlarms) {
        val alarmCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (alarm.repeatDays.isEmpty()) {
            if (alarmCal.timeInMillis <= nowMillis) {
                alarmCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            val diff = alarmCal.timeInMillis - nowMillis
            if (diff < minDiff) minDiff = diff
        } else {
            var bestOffset = 7
            val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK)
            val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            val alarmMinutes = alarm.hour * 60 + alarm.minute

            for (dayStr in alarm.repeatDays) {
                val targetDay = when (dayStr) {
                    "Mon" -> Calendar.MONDAY
                    "Tue" -> Calendar.TUESDAY
                    "Wed" -> Calendar.WEDNESDAY
                    "Thu" -> Calendar.THURSDAY
                    "Fri" -> Calendar.FRIDAY
                    "Sat" -> Calendar.SATURDAY
                    "Sun" -> Calendar.SUNDAY
                    else -> -1
                }
                if (targetDay != -1) {
                    var offset = (targetDay - currentDayOfWeek + 7) % 7
                    if (offset == 0 && alarmMinutes <= currentMinutes) {
                        offset = 7
                    }
                    if (offset < bestOffset) {
                        bestOffset = offset
                    }
                }
            }
            alarmCal.add(Calendar.DAY_OF_YEAR, bestOffset)
            val diff = alarmCal.timeInMillis - nowMillis
            if (diff < minDiff) minDiff = diff
        }
    }

    if (minDiff == Long.MAX_VALUE) return "All alarms are off"

    val diffMinutesTotal = minDiff / (1000 * 60)
    val days = diffMinutesTotal / (24 * 60)
    val hours = (diffMinutesTotal % (24 * 60)) / 60
    val minutes = diffMinutesTotal % 60

    val sb = StringBuilder("Alarm in ")
    if (days > 0) sb.append("$days days ")
    if (hours > 0) sb.append("$hours hours ")
    if (minutes > 0) sb.append("$minutes minutes")
    if (days == 0L && hours == 0L && minutes == 0L) sb.append("less than a minute")

    return sb.toString().trim()
}

private fun formatTime(hour: Int, minute: Int): String {
    return String.format("%02d:%02d", hour, minute)
}