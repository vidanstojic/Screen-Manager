package com.example.screenmanager.ui.feature.alarms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.screenmanager.alarm.domain.WakeCheckPolicy
import com.example.screenmanager.model.WakeCheckConfig
import com.example.screenmanager.ui.components.AppCard
import com.example.screenmanager.ui.components.CardDivider
import com.example.screenmanager.ui.components.SegmentedControl
import com.example.screenmanager.ui.components.SwitchRow
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing

/** Wake-up check: posle 5/10 min pitanje "Are you awake?"; bez potvrde za 2 min alarm ponovo zvoni. */
@Composable
fun AlarmWakeCheckSection(config: WakeCheckConfig, onChange: (WakeCheckConfig) -> Unit) {
    EditorSection(title = "Wake-up check") {
        AppCard(contentPadding = PaddingValues(vertical = Spacing.xs)) {
            SwitchRow(
                title = "Check that I'm really up",
                subtitle = if (config.enabled) {
                    "Asks ${config.delayMinutes} min after you dismiss"
                } else {
                    "Off"
                },
                checked = config.enabled,
                onCheckedChange = { onChange(config.copy(enabled = it)) }
            )
            if (config.enabled) {
                CardDivider()
                Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
                    Text("Ask after", style = MaterialTheme.typography.titleSmall, color = AppTheme.colors.textPrimary)
                    SegmentedControl(
                        options = WakeCheckConfig.DELAY_OPTIONS,
                        selected = WakeCheckConfig.DELAY_OPTIONS.minBy { kotlin.math.abs(it - config.delayMinutes) },
                        onSelected = { onChange(config.copy(delayMinutes = it)) },
                        label = { "$it min" },
                        modifier = Modifier.padding(top = Spacing.sm)
                    )
                }
            }
        }
        if (config.enabled) {
            EditorHint(
                "If you don't tap \"I'm awake\" within ${WakeCheckConfig.RESPONSE_WINDOW_SECONDS / 60} minutes, " +
                    "the alarm rings again (with its mission). Up to ${WakeCheckPolicy.MAX_ROUNDS} checks per morning."
            )
        }
    }
}
