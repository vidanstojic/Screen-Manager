package com.example.screenmanager.ui.settings.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.screenmanager.model.SessionLimitRule
import com.example.screenmanager.ui.components.SectionCard
import com.example.screenmanager.ui.components.StatusChip
import com.example.screenmanager.ui.theme.GlassTheme

/**
 * Interval mod (TRS 2.4): M minuta po sesiji, N sesija dnevno, K minuta pauze.
 */
@Composable
fun SessionLimitRulesSection(
    rules: List<SessionLimitRule>,
    onRuleChange: (SessionLimitRule) -> Unit,
    onAddRule: () -> Unit,
    onEditApps: (SessionLimitRule) -> Unit,
    onRemoveRule: (String) -> Unit
) {
    SectionCard(
        title = "Session intervals",
        subtitle = "Allow short sessions only: M minutes per session, N sessions per day, K minutes of cool-down after each session."
    ) {
        if (rules.isEmpty()) {
            Text("No interval rules yet.", color = GlassTheme.colors.textSecondary)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rules.forEach { rule ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GlassTheme.colors.surfaceElevated),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(1.dp, GlassTheme.colors.borderEnd, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(rule.name, color = GlassTheme.colors.textPrimary)
                                    Text(
                                        "${rule.maxSessions} × ${rule.sessionLengthMinutes}m • ${rule.cooldownMinutes}m pause",
                                        color = GlassTheme.colors.textSecondary
                                    )
                                }
                                Switch(
                                    checked = rule.isEnabled,
                                    onCheckedChange = { onRuleChange(rule.copy(isEnabled = it)) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = GlassTheme.colors.textPrimary,
                                        checkedTrackColor = GlassTheme.colors.accentPrimary
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            StepperRow("Session length (M)", rule.sessionLengthMinutes, "m", 1..120, 1) {
                                onRuleChange(rule.copy(sessionLengthMinutes = it))
                            }
                            StepperRow("Sessions per day (N)", rule.maxSessions, "", 1..50, 1) {
                                onRuleChange(rule.copy(maxSessions = it))
                            }
                            StepperRow("Cool-down (K)", rule.cooldownMinutes, "m", 0..240, 5) {
                                onRuleChange(rule.copy(cooldownMinutes = it))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusChip(text = "${rule.selectedAppIds.size} apps", isActive = rule.selectedAppIds.isNotEmpty())
                                TextButton(onClick = { onEditApps(rule) }) {
                                    Text("Apps", color = GlassTheme.colors.accentPrimary)
                                }
                                TextButton(onClick = { onRemoveRule(rule.id) }) {
                                    Text("Remove", color = GlassTheme.colors.accentSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = GlassTheme.colors.borderEnd)
        TextButton(onClick = onAddRule) {
            Text("Add interval rule", color = GlassTheme.colors.accentPrimary)
        }
    }
}

@Composable
internal fun StepperRow(
    label: String,
    value: Int,
    unit: String,
    range: IntRange,
    step: Int,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = GlassTheme.colors.textSecondary, modifier = Modifier.weight(1f))
        TextButton(
            onClick = { onChange((value - step).coerceIn(range)) },
            enabled = value > range.first
        ) { Text("−", color = GlassTheme.colors.textPrimary) }
        Text("$value$unit", color = GlassTheme.colors.textPrimary)
        TextButton(
            onClick = { onChange((value + step).coerceIn(range)) },
            enabled = value < range.last
        ) { Text("+", color = GlassTheme.colors.textPrimary) }
    }
}
