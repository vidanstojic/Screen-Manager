package com.example.screenmanager.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.screenmanager.model.ShortVideoConfig
import com.example.screenmanager.model.ShortsMode
import com.example.screenmanager.ui.components.SectionCard
import com.example.screenmanager.ui.components.StatusChip
import com.example.screenmanager.ui.components.ToggleChip
import com.example.screenmanager.ui.theme.GlassTheme

/**
 * Shorts/Reels pravilo: bira se MOD (potpuna blokada / dnevni budžet / sesije)
 * i njegovi parametri. Ostatak aplikacije (obični video, poruke) uvek radi.
 */
@Composable
fun ShortVideoSection(
    config: ShortVideoConfig,
    onConfigChange: (ShortVideoConfig) -> Unit,
    onSelectAppsClick: () -> Unit
) {
    SectionCard(
        title = "Shorts and Reels",
        subtitle = "Blocks only short-form video (YouTube Shorts, Instagram Reels). Regular videos, feed and messages keep working."
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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

        Spacer(modifier = Modifier.height(8.dp))
        Text("Mode", color = GlassTheme.colors.textSecondary)
        Spacer(modifier = Modifier.height(6.dp))
        ShortsModePicker(selected = config.mode) { onConfigChange(config.copy(mode = it)) }

        Spacer(modifier = Modifier.height(10.dp))
        when (config.mode) {
            ShortsMode.BLOCKED -> Text(
                "Every time Shorts/Reels open, you are sent back immediately.",
                color = GlassTheme.colors.textSecondary,
                fontSize = 13.sp
            )

            ShortsMode.BUDGET -> {
                StepperRow("Daily Shorts budget", config.maxReelsWatchMinutes, "m", 0..120, 5) {
                    onConfigChange(config.copy(maxReelsWatchMinutes = it))
                }
                StepperRow("Whole-app block after budget", config.fullAppBlockMinutes, "m", 5..240, 5) {
                    onConfigChange(config.copy(fullAppBlockMinutes = it))
                }
                Text(
                    "After the budget, the whole app is blocked for the set time and Shorts/Reels stay closed until midnight.",
                    color = GlassTheme.colors.textSecondary,
                    fontSize = 13.sp
                )
            }

            ShortsMode.SESSIONS -> {
                StepperRow("Session length (M)", config.sessionLengthMinutes, "m", 1..60, 1) {
                    onConfigChange(config.copy(sessionLengthMinutes = it))
                }
                StepperRow("Sessions per day (N)", config.maxSessions, "", 1..30, 1) {
                    onConfigChange(config.copy(maxSessions = it))
                }
                StepperRow("Pause after a session (K)", config.cooldownMinutes, "m", 0..240, 5) {
                    onConfigChange(config.copy(cooldownMinutes = it))
                }
                Text(
                    "Only time inside Shorts/Reels counts. During the pause and after the last session (until midnight) Shorts/Reels are closed; the rest of the app keeps working.",
                    color = GlassTheme.colors.textSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = GlassTheme.colors.borderEnd)
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusChip(text = "${config.selectedAppIds.size} apps", isActive = config.selectedAppIds.isNotEmpty())
            TextButton(onClick = onSelectAppsClick) {
                Text("Select apps", color = GlassTheme.colors.accentPrimary)
            }
        }
        Text(
            text = if (config.selectedAppIds.isEmpty()) "No apps selected yet." else config.selectedAppIds.joinToString(),
            color = GlassTheme.colors.textSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Detection currently supports YouTube and Instagram.",
            color = GlassTheme.colors.textSecondary,
            fontSize = 12.sp
        )
    }
}

@Composable
fun ShortsModePicker(selected: ShortsMode, onSelected: (ShortsMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ShortsMode.entries.forEach { mode ->
            ToggleChip(text = mode.label, selected = selected == mode, onClick = { onSelected(mode) })
        }
    }
}

val ShortsMode.label: String
    get() = when (this) {
        ShortsMode.BLOCKED -> "Block"
        ShortsMode.BUDGET -> "Daily budget"
        ShortsMode.SESSIONS -> "Sessions"
    }
