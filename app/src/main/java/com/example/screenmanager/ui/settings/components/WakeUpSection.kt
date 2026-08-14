package com.example.screenmanager.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.screenmanager.model.WakeUpConfig
import com.example.screenmanager.ui.components.SectionCard
import com.example.screenmanager.ui.components.StatusChip
import com.example.screenmanager.ui.theme.GlassTheme

@Composable
fun WakeUpSection(
    config: WakeUpConfig,
    onConfigChange: (WakeUpConfig) -> Unit,
    onSelectAppsClick: () -> Unit
) {
    SectionCard(
        title = "Wake-up blocking",
        subtitle = "After the phone stays inactive for the chosen time, configured apps are blocked when the user wakes it up."
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Enabled", color = GlassTheme.colors.textPrimary)
            Switch(
                checked = config.isEnabled,
                onCheckedChange = { onConfigChange(config.copy(isEnabled = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = GlassTheme.colors.textPrimary,
                    checkedTrackColor = GlassTheme.colors.accentPrimary
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(text = "${config.inactivityHours}h inactivity", isActive = true)
            StatusChip(text = "${config.blockDurationMinutes}m block", isActive = config.isEnabled)
            StatusChip(text = "${config.selectedAppIds.size} apps", isActive = config.selectedAppIds.isNotEmpty())
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Inactivity threshold", color = GlassTheme.colors.textSecondary)
        Slider(
            value = config.inactivityHours.toFloat(),
            onValueChange = { onConfigChange(config.copy(inactivityHours = it.toInt())) },
            valueRange = 1f..12f,
            steps = 11,
            colors = SliderDefaults.colors(
                thumbColor = GlassTheme.colors.accentPrimary,
                activeTrackColor = GlassTheme.colors.accentPrimary,
                inactiveTrackColor = GlassTheme.colors.surfaceElevated
            )
        )

        Text("Block duration", color = GlassTheme.colors.textSecondary)
        Slider(
            value = config.blockDurationMinutes.toFloat(),
            onValueChange = { onConfigChange(config.copy(blockDurationMinutes = it.toInt())) },
            valueRange = 5f..120f,
            steps = 23,
            colors = SliderDefaults.colors(
                thumbColor = GlassTheme.colors.accentPrimary,
                activeTrackColor = GlassTheme.colors.accentPrimary,
                inactiveTrackColor = GlassTheme.colors.surfaceElevated
            )
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = GlassTheme.colors.borderEnd)
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (config.selectedAppIds.isEmpty()) "No apps selected yet." else "Selected apps: ${config.selectedAppIds.joinToString()}",
            color = GlassTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Choose the apps that should react to wake-up blocking.",
            color = GlassTheme.colors.textSecondary
        )
        TextButton(onClick = onSelectAppsClick) {
            Text("Select apps", color = GlassTheme.colors.accentPrimary)
        }
    }
}