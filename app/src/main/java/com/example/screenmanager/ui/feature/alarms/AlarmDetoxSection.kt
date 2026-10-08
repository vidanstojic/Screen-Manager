package com.example.screenmanager.ui.feature.alarms

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.screenmanager.alarm.domain.SiteRules
import com.example.screenmanager.alarm.domain.WakeUpDetoxPolicy
import com.example.screenmanager.model.AppOption
import com.example.screenmanager.model.WakeUpDetox
import com.example.screenmanager.ui.common.formatMinutes
import com.example.screenmanager.ui.components.AppCard
import com.example.screenmanager.ui.components.AppPickerDialog
import com.example.screenmanager.ui.components.AppSelectionField
import com.example.screenmanager.ui.components.AppTextField
import com.example.screenmanager.ui.components.CardDivider
import com.example.screenmanager.ui.components.CircleIconButton
import com.example.screenmanager.ui.components.IconBadge
import com.example.screenmanager.ui.components.ListRow
import com.example.screenmanager.ui.components.StatusPill
import com.example.screenmanager.ui.components.StepperRow
import com.example.screenmanager.ui.components.SwitchRow
import com.example.screenmanager.ui.components.TextAction
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing

private val SUGGESTED_SITES = listOf("instagram.com", "youtube.com", "tiktok.com", "x.com", "facebook.com", "reddit.com")

/**
 * Wake-up detox: posle gašenja alarma blokira izabrane aplikacije i sajtove
 * na zadato vreme.
 */
@Composable
fun AlarmDetoxSection(
    detox: WakeUpDetox,
    onChange: (WakeUpDetox) -> Unit,
    installedApps: List<AppOption>,
    labelFor: (String) -> String,
    readiness: DetoxReadiness
) {
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    EditorSection(title = "Wake-up detox") {
        AppCard(contentPadding = PaddingValues(vertical = Spacing.xs)) {
            SwitchRow(
                title = "Block apps & sites after waking up",
                subtitle = if (detox.enabled) "For ${formatMinutes(detox.minutes)} after you dismiss the alarm" else "Off",
                checked = detox.enabled,
                onCheckedChange = { onChange(detox.copy(enabled = it)) }
            )
            if (detox.enabled) {
                CardDivider()
                StepperRow(
                    label = "Duration",
                    value = detox.minutes.coerceIn(WakeUpDetoxPolicy.MIN_MINUTES, WakeUpDetoxPolicy.MAX_MINUTES),
                    onValueChange = { onChange(detox.copy(minutes = it)) },
                    range = WakeUpDetoxPolicy.MIN_MINUTES..WakeUpDetoxPolicy.MAX_MINUTES,
                    step = 5,
                    valueLabel = ::formatMinutes
                )
                CardDivider()
                AppSelectionField(
                    selectedIds = detox.apps,
                    labelFor = labelFor,
                    onClick = { pickerOpen = true }
                )
                CardDivider()
                SitesEditor(sites = detox.sites, onSitesChange = { onChange(detox.copy(sites = it)) })
            }
        }
        if (detox.enabled) {
            DetoxWarnings(detox, readiness)
            EditorHint("Apps show the block screen; blocked sites in your browser send you back to the home screen.")
        }
    }
    if (pickerOpen) {
        AppPickerDialog(
            availableApps = installedApps,
            initialSelectedIds = detox.apps,
            onDismiss = { pickerOpen = false },
            onConfirm = { ids ->
                onChange(detox.copy(apps = ids))
                pickerOpen = false
            }
        )
    }
}

@Composable
private fun SitesEditor(sites: List<String>, onSitesChange: (List<String>) -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    val normalized = SiteRules.normalize(input)
    val add = {
        if (normalized != null && normalized !in sites) onSitesChange(sites + normalized)
        input = ""
    }
    Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
        Text("Websites", style = MaterialTheme.typography.titleSmall, color = AppTheme.colors.textPrimary)
        sites.forEach { site ->
            ListRow(
                title = site,
                modifier = Modifier.padding(start = 0.dp),
                leading = { IconBadge(icon = Icons.Rounded.Language, size = 28.dp) },
                trailing = {
                    CircleIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = "Remove $site",
                        onClick = { onSitesChange(sites - site) }
                    )
                }
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppTextField(
                value = input,
                onValueChange = { input = it.trim() },
                label = "Add a site",
                placeholder = "e.g. instagram.com",
                modifier = Modifier.weight(1f)
            )
            CircleIconButton(
                icon = Icons.Rounded.Add,
                contentDescription = "Add site",
                onClick = add,
                enabled = normalized != null,
                modifier = Modifier.padding(start = Spacing.sm)
            )
        }
        if (input.isNotEmpty() && normalized == null) {
            Text("Enter a domain like youtube.com", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.warning)
        }
        val suggestions = SUGGESTED_SITES.filterNot { it in sites }
        if (suggestions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(top = Spacing.sm)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                suggestions.forEach { site ->
                    StatusPill(
                        text = "+ $site",
                        color = AppTheme.colors.accent,
                        modifier = Modifier.clickable { onSitesChange(sites + site) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetoxWarnings(detox: WakeUpDetox, readiness: DetoxReadiness) {
    val context = LocalContext.current
    if (detox.apps.isEmpty() && detox.sites.isEmpty()) {
        EditorHint("Choose at least one app or site.", color = AppTheme.colors.warning)
    }
    if (detox.apps.isNotEmpty() && !readiness.appsEnforced) {
        WarningRow("Blocking apps needs Usage access and Display over other apps (Screen Manager › Settings).")
    }
    if (detox.sites.isNotEmpty() && !readiness.sitesEnforced) {
        WarningRow("Blocking sites needs the Accessibility service.") {
            TextAction(text = "Turn on", onClick = {
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            })
        }
    }
}

@Composable
private fun WarningRow(text: String, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.padding(horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.colors.warning,
            modifier = Modifier.weight(1f)
        )
        action?.invoke()
    }
}
