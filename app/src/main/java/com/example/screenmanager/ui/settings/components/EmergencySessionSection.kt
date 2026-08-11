package com.example.screenmanager.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.screenmanager.model.EmergencySessionConfig
import com.example.screenmanager.ui.components.SectionCard
import com.example.screenmanager.ui.components.StatusChip

/**
 * Sekcija za emergency session konfiguraciju.
 *
 * Korisnik odavde kontroliše privremeni bypass svih blokada i trajanje sesije.
 */
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
            Text("Manual override enabled")
            Switch(
                checked = config.manualEndEnabled,
                onCheckedChange = { onConfigChange(config.copy(manualEndEnabled = it)) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(text = "Default ${config.defaultDurationMinutes}m", isActive = true)
            StatusChip(text = if (config.isActive) "Active" else "Inactive", isActive = config.isActive)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Default session duration")
        Slider(
            value = config.defaultDurationMinutes.toFloat(),
            onValueChange = { onConfigChange(config.copy(defaultDurationMinutes = it.toInt())) },
            valueRange = 5f..120f,
            steps = 23
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        if (config.activeUntilLabel.isNullOrBlank()) {
            Text("No active emergency session.")
        } else {
            Text("Active until ${config.activeUntilLabel}")
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onActivateSession, modifier = Modifier.weight(1f)) {
                Text("Activate")
            }
            Button(
                onClick = onEndSession,
                enabled = config.isActive,
                modifier = Modifier.weight(1f)
            ) {
                Text("End session")
            }
        }
    }
}
