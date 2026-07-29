package com.example.screenmanager.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.screenmanager.model.ShortVideoConfig
import com.example.screenmanager.ui.components.SectionCard
import com.example.screenmanager.ui.components.StatusChip

@Composable
fun ShortVideoSection(
    config: ShortVideoConfig,
    onConfigChange: (ShortVideoConfig) -> Unit,
    onSelectAppsClick: () -> Unit
) {
    SectionCard(
        title = "Shorts and Reels",
        subtitle = "Limit short-form content for YouTube and Instagram, then block the app for the configured cooldown."
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Enabled")
            Switch(
                checked = config.isEnabled,
                onCheckedChange = { onConfigChange(config.copy(isEnabled = it)) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(text = "${config.maxReelsWatchMinutes}m shorts limit", isActive = true)
            StatusChip(text = "${config.fullAppBlockMinutes}m block", isActive = config.isEnabled)
            StatusChip(text = "${config.selectedAppIds.size} apps", isActive = config.selectedAppIds.isNotEmpty())
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Short-form limit")
        Slider(
            value = config.maxReelsWatchMinutes.toFloat(),
            onValueChange = { onConfigChange(config.copy(maxReelsWatchMinutes = it.toInt())) },
            valueRange = 5f..60f,
            steps = 11
        )

        Text("Penalty block")
        Slider(
            value = config.fullAppBlockMinutes.toFloat(),
            onValueChange = { onConfigChange(config.copy(fullAppBlockMinutes = it.toInt())) },
            valueRange = 15f..120f,
            steps = 7
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (config.selectedAppIds.isEmpty()) "No apps selected yet." else "Selected apps: ${config.selectedAppIds.joinToString()}",
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "YouTube and Instagram are preselected, but you can choose more apps if needed."
        )
        TextButton(onClick = onSelectAppsClick) {
            Text("Select apps")
        }
    }
}
