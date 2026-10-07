package com.example.screenmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.screenmanager.ui.common.formatDuration
import com.example.screenmanager.ui.model.AppUsageItem
import com.example.screenmanager.ui.model.DayOption
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.GlowAlpha
import com.example.screenmanager.ui.theme.Spacing
import com.example.screenmanager.ui.theme.glass
import com.example.screenmanager.ui.theme.glow

/**
 * Red liste potrošnje: ikonica, ime, vreme i traka udela u odnosu na
 * najkorišćeniju aplikaciju ([maxDurationMs]). Koriste ga Overview i Stats.
 */
@Composable
fun AppUsageRow(
    app: AppUsageItem,
    maxDurationMs: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(packageName = app.packageName, label = app.label)
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = formatDuration(app.durationMs),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppTheme.colors.textSecondary
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            ProportionBar(
                fraction = if (maxDurationMs > 0) app.durationMs.toFloat() / maxDurationMs else 0f
            )
        }
    }
}

/** Traka sa poslednjih 7 dana; izabrani dan je u akcentnom gradijentu sa sjajem. */
@Composable
fun DayStrip(
    days: List<DayOption>,
    selectedDayStart: Long,
    onDaySelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        days.forEach { day ->
            val isSelected = day.dayStart == selectedDayStart
            val shape = MaterialTheme.shapes.medium
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (isSelected) {
                            Modifier
                                .glow(colors.accentStart.copy(alpha = GlowAlpha), cornerRadius = 16.dp, blurRadius = 12.dp)
                                .clip(shape)
                                .background(colors.accentBrush)
                        } else {
                            Modifier.glass(colors, shape)
                        }
                    )
                    .clickable { onDaySelected(day.dayStart) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = day.weekday,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) AppTheme.colors.onAccent else AppTheme.colors.textSecondary,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = day.dayOfMonth,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) AppTheme.colors.onAccent else AppTheme.colors.textPrimary,
                    maxLines = 1
                )
            }
        }
    }
}
