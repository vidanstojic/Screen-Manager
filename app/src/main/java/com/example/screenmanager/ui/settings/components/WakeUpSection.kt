package com.example.screenmanager.ui.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.screenmanager.model.WakeUpConfig

@Composable
fun WakeUpSection(
    config: WakeUpConfig,
    onConfigChange: (WakeUpConfig) -> Unit,
    onSelectAppsClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Detekcija buđenja", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Sati neaktivnosti: ${config.inactivityHours}h")
            Slider(
                value = config.inactivityHours.toFloat(),
                onValueChange = { onConfigChange(config.copy(inactivityHours = it.toInt())) },
                valueRange = 1f..12f,
                steps = 11
            )

            Text(text = "Trajanje blokade: ${config.blockDurationMinutes} min")
            Slider(
                value = config.blockDurationMinutes.toFloat(),
                onValueChange = { onConfigChange(config.copy(blockDurationMinutes = it.toInt())) },
                valueRange = 5f..120f,
                steps = 23
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = onSelectAppsClick, modifier = Modifier.align(Alignment.End)) {
                Text("Izaberi aplikacije (${config.selectedAppIds.size})")
            }
        }
    }
}
