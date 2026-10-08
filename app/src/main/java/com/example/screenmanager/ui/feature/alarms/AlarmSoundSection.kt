package com.example.screenmanager.ui.feature.alarms

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.screenmanager.alarm.data.AlarmTones
import com.example.screenmanager.alarm.data.CustomTones
import com.example.screenmanager.alarm.data.TonePreview
import com.example.screenmanager.model.AlarmSound
import com.example.screenmanager.model.SnoozeConfig
import com.example.screenmanager.ui.common.pluralize
import com.example.screenmanager.ui.components.AppCard
import com.example.screenmanager.ui.components.CardDivider
import com.example.screenmanager.ui.components.ChevronIcon
import com.example.screenmanager.ui.components.IconBadge
import com.example.screenmanager.ui.components.ListRow
import com.example.screenmanager.ui.components.SegmentedControl
import com.example.screenmanager.ui.components.SliderRow
import com.example.screenmanager.ui.components.StepperRow
import com.example.screenmanager.ui.components.SwitchRow
import com.example.screenmanager.ui.components.TextAction
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing

private val FADE_OPTIONS = listOf(0, 15, 30, 60)

/** Zvuk: melodija, jačina, postepeno pojačavanje, vibracija. */
@Composable
fun AlarmSoundSection(sound: AlarmSound, onChange: (AlarmSound) -> Unit) {
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    EditorSection(title = "Sound") {
        AppCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = Spacing.xs)) {
            ListRow(
                title = "Tone",
                subtitle = AlarmTones.displayName(sound),
                onClick = { pickerOpen = true },
                leading = { IconBadge(icon = Icons.Rounded.MusicNote, size = 36.dp) },
                trailing = { ChevronIcon() }
            )
            CardDivider()
            SliderRow(
                label = "Volume",
                value = sound.volumePercent,
                onValueChange = { onChange(sound.copy(volumePercent = it)) },
                range = 10..100,
                step = 5,
                valueLabel = { "$it%" }
            )
            CardDivider()
            Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
                Text("Gradually louder", style = MaterialTheme.typography.titleSmall, color = AppTheme.colors.textPrimary)
                Text(
                    text = "Starts quiet and reaches full volume after the chosen time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary,
                    modifier = Modifier.padding(bottom = Spacing.sm)
                )
                SegmentedControl(
                    options = FADE_OPTIONS,
                    selected = FADE_OPTIONS.minBy { kotlin.math.abs(it - sound.fadeInSeconds) },
                    onSelected = { onChange(sound.copy(fadeInSeconds = it)) },
                    label = { if (it == 0) "Off" else "$it s" }
                )
            }
            CardDivider()
            SwitchRow(
                title = "Vibrate",
                checked = sound.vibrate,
                onCheckedChange = { onChange(sound.copy(vibrate = it)) }
            )
        }
    }
    if (pickerOpen) {
        TonePickerDialog(
            current = sound,
            onDismiss = { pickerOpen = false },
            onConfirm = { toneId, customName ->
                onChange(sound.copy(toneId = toneId, customName = customName))
                pickerOpen = false
            }
        )
    }
}

/** Odlaganje: uključeno/isključeno, trajanje i najveći broj odlaganja zaredom. */
@Composable
fun AlarmSnoozeSection(snooze: SnoozeConfig, onChange: (SnoozeConfig) -> Unit) {
    EditorSection(title = "Snooze") {
        AppCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = Spacing.xs)) {
            SwitchRow(
                title = "Allow snooze",
                subtitle = if (snooze.enabled) {
                    "${snooze.minutes} min, up to ${pluralize(snooze.maxCount, "time")}"
                } else {
                    "The alarm can only be dismissed"
                },
                checked = snooze.enabled,
                onCheckedChange = { onChange(snooze.copy(enabled = it)) }
            )
            if (snooze.enabled) {
                CardDivider()
                StepperRow(
                    label = "Length",
                    value = snooze.minutes,
                    onValueChange = { onChange(snooze.copy(minutes = it)) },
                    range = 1..30,
                    valueLabel = { "$it min" }
                )
                CardDivider()
                StepperRow(
                    label = "Max snoozes",
                    value = snooze.maxCount,
                    onValueChange = { onChange(snooze.copy(maxCount = it)) },
                    range = 1..10
                )
            }
        }
    }
}

/**
 * Izbor melodije: zvuci aplikacije, sistemski zvuk ili fajl sa telefona.
 * Dodir pušta probu; izbor se potvrđuje na "Done".
 */
@Composable
private fun TonePickerDialog(
    current: AlarmSound,
    onDismiss: () -> Unit,
    onConfirm: (toneId: String, customName: String?) -> Unit
) {
    val context = LocalContext.current
    val preview = remember { TonePreview(context) }
    DisposableEffect(Unit) { onDispose { preview.stop() } }

    var selectedId by remember { mutableStateOf(current.toneId) }
    var customName by remember { mutableStateOf(current.customName) }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val tone = CustomTones.import(context, uri)
            selectedId = tone.toneId
            customName = tone.name
            preview.play(tone.toneId)
        }
    }
    val select: (String) -> Unit = { id ->
        selectedId = id
        preview.play(id)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.colors.surfaceHigh,
        title = { Text("Alarm tone", color = AppTheme.colors.textPrimary) },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                AlarmTones.builtins.forEach { tone ->
                    ToneRow(tone.name, tone.description, Icons.Rounded.MusicNote, selectedId == tone.id) { select(tone.id) }
                }
                ToneRow("System default", "Your phone's alarm sound", Icons.Rounded.PhoneAndroid, selectedId == AlarmTones.SYSTEM_ID) {
                    select(AlarmTones.SYSTEM_ID)
                }
                if (selectedId.startsWith(AlarmTones.CUSTOM_PREFIX)) {
                    ToneRow(customName ?: "Your sound", "From your files", Icons.Rounded.FolderOpen, true) { select(selectedId) }
                }
                ToneRow("Choose a file…", "Any song or sound on your phone", Icons.Rounded.FolderOpen, false) {
                    preview.stop()
                    fileLauncher.launch(arrayOf("audio/*"))
                }
            }
        },
        confirmButton = {
            TextAction(text = "Done", onClick = {
                onConfirm(selectedId, customName.takeIf { selectedId.startsWith(AlarmTones.CUSTOM_PREFIX) })
            })
        },
        dismissButton = { TextAction(text = "Cancel", color = AppTheme.colors.textSecondary, onClick = onDismiss) }
    )
}

@Composable
private fun ToneRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    ListRow(
        title = title,
        subtitle = subtitle,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        leading = { IconBadge(icon = icon, size = 32.dp, tint = if (selected) AppTheme.colors.accent else AppTheme.colors.textSecondary) },
        trailing = if (selected) {
            { Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = AppTheme.colors.accent) }
        } else {
            null
        }
    )
}
