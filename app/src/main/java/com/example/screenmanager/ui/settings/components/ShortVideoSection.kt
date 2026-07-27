package com.example.screenmanager.ui.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.screenmanager.model.ShortVideoConfig

@Composable
fun ShortVideoSection(
    config: ShortVideoConfig,
    onConfigChange: (ShortVideoConfig) -> Unit,
    onSelectAppsClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Reels & Shorts Limit", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Maksimalno gledanje: ${config.maxReelsWatchMinutes} min")
            Slider(
                value = config.maxReelsWatchMinutes.toFloat(),
                onValueChange = { onConfigChange(config.copy(maxReelsWatchMinutes = it.toInt())) },
                valueRange = 5f..60f,
                steps = 11
            )

            Text(text = "Blokada cele aplikacije: ${config.fullAppBlockMinutes} min")
            Slider(
                value = config.fullAppBlockMinutes.toFloat(),
                onValueChange = { onConfigChange(config.copy(fullAppBlockMinutes = it.toInt())) },
                valueRange = 15f..120f,
                steps = 7
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = onSelectAppsClick, modifier = Modifier.align(Alignment.End)) {
                Text("Izaberi aplikacije (${config.selectedAppIds.size})")
            }
        }
    }
}