package com.example.screenmanager.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.screenmanager.model.EmergencySessionConfig
import com.example.screenmanager.ui.components.SectionCard
import com.example.screenmanager.ui.components.StatusChip
import com.example.screenmanager.ui.theme.GlassTheme

@Composable
fun EmergencySessionSection(
    config: EmergencySessionConfig,
    onConfigChange: (EmergencySessionConfig) -> Unit,
    onActivateSession: () -> Unit,
    onEndSession: () -> Unit
) {
    SectionCard(
        title = "Emergency session",
        subtitle = "Temporarily bypass all blocking rules for a limited time."
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Manual override enabled", color = GlassTheme.colors.textPrimary)
            Switch(
                checked = config.manualEndEnabled,
                onCheckedChange = { onConfigChange(config.copy(manualEndEnabled = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = GlassTheme.colors.textPrimary,
                    checkedTrackColor = GlassTheme.colors.accentPrimary
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(text = "Default ${config.defaultDurationMinutes}m", isActive = true)
            StatusChip(text = if (config.isActive) "Active" else "Inactive", isActive = config.isActive)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Default session duration", color = GlassTheme.colors.textSecondary)
        Slider(
            value = config.defaultDurationMinutes.toFloat(),
            onValueChange = { onConfigChange(config.copy(defaultDurationMinutes = it.toInt())) },
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

        if (config.activeUntilLabel.isNullOrBlank()) {
            Text("No active emergency session.", color = GlassTheme.colors.textSecondary)
        } else {
            Text("Active until ${config.activeUntilLabel}", color = GlassTheme.colors.accentSecondary)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onActivateSession,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = GlassTheme.colors.accentPrimary)
            ) {
                Text("Activate", color = GlassTheme.colors.textPrimary)
            }
            Button(
                onClick = onEndSession,
                enabled = config.isActive,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlassTheme.colors.accentSecondary,
                    disabledContainerColor = GlassTheme.colors.surfaceElevated
                )
            ) {
                Text("End session", color = GlassTheme.colors.textPrimary)
            }
        }
    }
}