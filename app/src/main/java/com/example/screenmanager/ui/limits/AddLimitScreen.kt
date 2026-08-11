package com.example.screenmanager.ui.limits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.screenmanager.model.MockAppUsage
import com.example.screenmanager.model.AppOption
import com.example.screenmanager.model.AppUsageSummary
import com.example.screenmanager.ui.components.InfoBanner
import com.example.screenmanager.ui.components.SectionCard
import com.example.screenmanager.ui.components.StatusChip
import com.example.screenmanager.ui.components.ToggleChip
import com.example.screenmanager.ui.settings.components.AppPickerDialog

/**
 * Ekran za kreiranje i uređivanje limita aplikacije.
 *
 * Do njega se dolazi iz detalja aplikacije ili iz limits pregleda i ovde
 * korisnik bira aplikacije, tip pravila i trajanje blokade.
 */
@Composable
fun AddLimitScreen(
    selectedApp: AppUsageSummary?,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    var limitName by remember { mutableStateOf(selectedApp?.name?.let { "$it limit" } ?: "New limit") }
    var dailyLimitMinutes by remember { mutableStateOf(60) }
    var blockDurationMinutes by remember { mutableStateOf(60) }
    var allowEmergencySession by remember { mutableStateOf(true) }
    var selectedRuleType by remember { mutableStateOf("App limit") }
    var showPicker by remember { mutableStateOf(false) }
    var selectedAppIds by remember {
        mutableStateOf(
            selectedApp?.let { listOf(it.name.lowercase()) } ?: listOf("com.google.android.youtube")
        )
    }

    val availableApps = remember {
        listOf(
            AppOption("com.google.android.youtube", "YouTube", "Video", "YT"),
            AppOption("com.instagram.android", "Instagram", "Social", "IG"),
            AppOption("com.zhiliaoapp.musically", "TikTok", "Short video", "TT"),
            AppOption("com.android.chrome", "Chrome", "Browser", "CH"),
            AppOption("com.whatsapp", "WhatsApp", "Messaging", "WA")
        )
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("Add usage limit", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Create one rule that can block the app, add a penalty, and support emergency bypass.")
                }
            }

            InfoBanner(
                title = "Designed for all block types",
                description = "Use this editor for daily app limits, shorts/reels penalties, or future scheduled blocking templates."
            )

            SectionCard(title = "Target apps", subtitle = "Pick one or more apps for this limit.") {
                if (selectedApp != null) {
                    Text("Starting from ${selectedApp.name}")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(text = "${selectedAppIds.size} selected", isActive = selectedAppIds.isNotEmpty())
                    StatusChip(text = if (allowEmergencySession) "Emergency allowed" else "Emergency disabled", isActive = allowEmergencySession)
                }
                Button(onClick = { showPicker = true }) {
                    Text("Choose apps")
                }
            }

            SectionCard(title = "Rule type", subtitle = "Choose how the rule behaves.") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToggleChip(text = "App limit", selected = selectedRuleType == "App limit", onClick = { selectedRuleType = "App limit" })
                    ToggleChip(text = "Shorts/Reels", selected = selectedRuleType == "Shorts/Reels", onClick = { selectedRuleType = "Shorts/Reels" })
                    ToggleChip(text = "Scheduled", selected = selectedRuleType == "Scheduled", onClick = { selectedRuleType = "Scheduled" })
                }
            }

            SectionCard(title = "Limit values", subtitle = "Tune the daily usage cap and the penalty block.") {
                OutlinedTextField(
                    value = limitName,
                    onValueChange = { limitName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Limit name") }
                )
                Text("Daily limit: ${dailyLimitMinutes}m")
                Slider(
                    value = dailyLimitMinutes.toFloat(),
                    onValueChange = { dailyLimitMinutes = it.toInt() },
                    valueRange = 5f..480f,
                    steps = 94
                )
                Text("Penalty block: ${blockDurationMinutes}m")
                Slider(
                    value = blockDurationMinutes.toFloat(),
                    onValueChange = { blockDurationMinutes = it.toInt() },
                    valueRange = 5f..240f,
                    steps = 46
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Allow emergency session")
                    Switch(
                        checked = allowEmergencySession,
                        onCheckedChange = { allowEmergencySession = it }
                    )
                }
            }

            SectionCard(title = "Summary", subtitle = null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Rule: $selectedRuleType")
                    Text("Apps: ${selectedAppIds.joinToString()}")
                    Text("Daily cap: ${dailyLimitMinutes}m")
                    Text("Penalty block: ${blockDurationMinutes}m")
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }
        }
    }

    if (showPicker) {
        AppPickerDialog(
            availableApps = availableApps,
            initialSelectedIds = selectedAppIds,
            onDismiss = { showPicker = false },
            onConfirm = {
                selectedAppIds = it
                showPicker = false
            }
        )
    }
}
