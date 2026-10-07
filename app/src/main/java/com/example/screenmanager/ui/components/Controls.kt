package com.example.screenmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.screenmanager.ui.common.formatClock
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing
import java.time.DayOfWeek
import java.time.LocalTime
import kotlin.math.roundToInt

/** Switch u bojama teme — jedino mesto gde se te boje podešavaju. */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = AppTheme.colors.onAccent,
            checkedTrackColor = AppTheme.colors.accent,
            checkedBorderColor = AppTheme.colors.accent,
            uncheckedThumbColor = AppTheme.colors.textSecondary,
            uncheckedTrackColor = AppTheme.colors.surfaceRaised,
            uncheckedBorderColor = AppTheme.colors.outline
        )
    )
}

/** Red "naslov + opis + switch" — ceo red je klikabilan. */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null
) {
    ListRow(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        onClick = { onCheckedChange(!checked) },
        leading = leading,
        trailing = { AppSwitch(checked = checked, onCheckedChange = onCheckedChange) }
    )
}

/**
 * Brojčana vrednost sa − / + dugmadima (minuti, broj sesija...).
 * [valueLabel] određuje kako se vrednost ispisuje ("15m", "3").
 */
@Composable
fun StepperRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    step: Int = 1,
    supportingText: String? = null,
    valueLabel: (Int) -> String = { it.toString() }
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.titleSmall, color = AppTheme.colors.textPrimary)
            if (supportingText != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary
                )
            }
        }
        Spacer(Modifier.width(Spacing.md))
        CircleIconButton(
            icon = Icons.Rounded.Remove,
            contentDescription = "Decrease $label",
            onClick = { onValueChange((value - step).coerceIn(range.first, range.last)) },
            enabled = value > range.first
        )
        Text(
            text = valueLabel(value),
            style = MaterialTheme.typography.titleMedium,
            color = AppTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.widthIn(min = 60.dp)
        )
        CircleIconButton(
            icon = Icons.Rounded.Add,
            contentDescription = "Increase $label",
            onClick = { onValueChange((value + step).coerceIn(range.first, range.last)) },
            enabled = value < range.last
        )
    }
}

/** Klizač za veći opseg vrednosti (npr. dnevni limit 5 min – 8 h), sa ispisom vrednosti. */
@Composable
fun SliderRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    step: Int = 1,
    valueLabel: (Int) -> String = { it.toString() }
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueLabel(value),
                style = MaterialTheme.typography.titleMedium,
                color = AppTheme.colors.accent
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { raw ->
                val snapped = (raw / step).roundToInt() * step
                onValueChange(snapped.coerceIn(range.first, range.last))
            },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = AppTheme.colors.accent,
                activeTrackColor = AppTheme.colors.accent,
                inactiveTrackColor = AppTheme.colors.surfaceRaised
            )
        )
    }
}

/** Tekstualno polje u bojama teme. */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leadingIcon,
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AppTheme.colors.textPrimary,
            unfocusedTextColor = AppTheme.colors.textPrimary,
            focusedBorderColor = AppTheme.colors.accent,
            unfocusedBorderColor = AppTheme.colors.outline,
            focusedLabelColor = AppTheme.colors.accent,
            unfocusedLabelColor = AppTheme.colors.textSecondary,
            focusedPlaceholderColor = AppTheme.colors.textMuted,
            unfocusedPlaceholderColor = AppTheme.colors.textMuted,
            cursorColor = AppTheme.colors.accent,
            focusedLeadingIconColor = AppTheme.colors.textSecondary,
            unfocusedLeadingIconColor = AppTheme.colors.textSecondary
        )
    )
}

/** Sedam kružića M T W T F S S — izbor dana u nedelji (zakazana blokada, alarm). */
@Composable
fun WeekdaySelector(
    selected: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        DayOfWeek.entries.forEach { day ->
            val isSelected = day in selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(if (isSelected) AppTheme.colors.accent else AppTheme.colors.surfaceRaised)
                    .clickable { onToggle(day) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.name.take(1),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) AppTheme.colors.onAccent else AppTheme.colors.textSecondary
                )
            }
        }
    }
}

/** Polje koje prikazuje izabrano vreme; klik otvara [AppTimePickerDialog]. */
@Composable
fun TimeField(
    label: String,
    time: LocalTime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(AppTheme.colors.surfaceRaised)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.textSecondary)
        Spacer(Modifier.height(2.dp))
        Text(
            text = formatClock(time),
            style = MaterialTheme.typography.headlineSmall,
            color = AppTheme.colors.textPrimary
        )
    }
}

/** Dijalog za izbor vremena (24h), u bojama teme. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTimePickerDialog(
    title: String,
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.colors.surface,
        title = {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.textPrimary)
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = AppTheme.colors.surfaceRaised,
                        selectorColor = AppTheme.colors.accent,
                        timeSelectorSelectedContainerColor = AppTheme.colors.accentSoft,
                        timeSelectorSelectedContentColor = AppTheme.colors.accent,
                        timeSelectorUnselectedContainerColor = AppTheme.colors.surfaceRaised,
                        timeSelectorUnselectedContentColor = AppTheme.colors.textPrimary
                    )
                )
            }
        },
        confirmButton = {
            TextAction(text = "OK", onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) })
        },
        dismissButton = {
            TextAction(text = "Cancel", onClick = onDismiss, color = AppTheme.colors.textSecondary)
        }
    )
}
