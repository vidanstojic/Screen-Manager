package com.example.screenmanager.ui.limits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.screenmanager.ScreenManagerApplication
import com.example.screenmanager.domain.getInstalledApps
import com.example.screenmanager.model.AppLimitRule
import com.example.screenmanager.model.AppOption
import com.example.screenmanager.model.AppUsageSummary
import com.example.screenmanager.model.SessionLimitRule
import com.example.screenmanager.model.ShortsMode
import com.example.screenmanager.ui.components.InfoBanner
import com.example.screenmanager.ui.components.SectionCard
import com.example.screenmanager.ui.components.StatusChip
import com.example.screenmanager.ui.components.ToggleChip
import com.example.screenmanager.ui.settings.SettingsViewModel
import com.example.screenmanager.ui.settings.SettingsViewModelFactory
import com.example.screenmanager.ui.settings.components.AppPickerDialog
import com.example.screenmanager.ui.settings.components.ShortsModePicker
import com.example.screenmanager.ui.theme.GlassBackground
import com.example.screenmanager.ui.theme.GlassTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.roundToInt

private enum class RuleType(val label: String) {
    APP_LIMIT("App limit"),
    SESSIONS("Sessions"),
    SHORTS("Shorts/Reels")
}

/**
 * Ekran za kreiranje pravila.
 *
 * Do njega se dolazi iz detalja aplikacije ili iz limits pregleda. "Save"
 * sada zaista upisuje pravilo u bazu (ranije je samo zatvarao ekran), a
 * izbor aplikacije koristi packageName (ranije `name.lowercase()`).
 */
@Composable
fun AddLimitScreen(
    selectedApp: AppUsageSummary?,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(ScreenManagerApplication.getInstance().settingsRepository)
    )
) {
    val context = LocalContext.current

    var ruleType by remember { mutableStateOf(RuleType.APP_LIMIT) }
    var limitName by remember { mutableStateOf(selectedApp?.name?.let { "$it limit" } ?: "New limit") }
    var dailyLimitMinutes by remember { mutableStateOf(60) }
    var shortsBudgetMinutes by remember { mutableStateOf(15) }
    var shortsPenaltyMinutes by remember { mutableStateOf(60) }
    var shortsMode by remember { mutableStateOf(ShortsMode.SESSIONS) }
    var sessionLengthMinutes by remember { mutableStateOf(5) }
    var maxSessions by remember { mutableStateOf(5) }
    var cooldownMinutes by remember { mutableStateOf(15) }
    var showPicker by remember { mutableStateOf(false) }
    var selectedAppIds by remember {
        mutableStateOf(selectedApp?.let { listOf(it.packageName) } ?: emptyList())
    }
    var availableApps by remember { mutableStateOf<List<AppOption>>(emptyList()) }

    LaunchedEffect(Unit) {
        availableApps = withContext(Dispatchers.IO) { getInstalledApps(context) }
    }

    fun save() {
        when (ruleType) {
            RuleType.APP_LIMIT -> viewModel.updateAppLimitRule(
                AppLimitRule(
                    id = UUID.randomUUID().toString(),
                    name = limitName.ifBlank { "Daily limit" },
                    selectedAppIds = selectedAppIds,
                    dailyLimitMinutes = dailyLimitMinutes,
                    blockDurationMinutes = 0,
                    description = "Blocked until midnight once the daily cap is used."
                )
            )

            RuleType.SESSIONS -> viewModel.upsertSessionLimitRule(
                SessionLimitRule(
                    id = UUID.randomUUID().toString(),
                    name = limitName.ifBlank { "Interval rule" },
                    selectedAppIds = selectedAppIds,
                    sessionLengthMinutes = sessionLengthMinutes,
                    maxSessions = maxSessions,
                    cooldownMinutes = cooldownMinutes
                )
            )

            RuleType.SHORTS -> viewModel.mergeShortVideoConfig(additionalAppIds = selectedAppIds) { current ->
                when (shortsMode) {
                    ShortsMode.BLOCKED -> current.copy(mode = ShortsMode.BLOCKED)
                    ShortsMode.BUDGET -> current.copy(
                        mode = ShortsMode.BUDGET,
                        maxReelsWatchMinutes = shortsBudgetMinutes,
                        fullAppBlockMinutes = shortsPenaltyMinutes
                    )
                    ShortsMode.SESSIONS -> current.copy(
                        mode = ShortsMode.SESSIONS,
                        sessionLengthMinutes = sessionLengthMinutes,
                        maxSessions = maxSessions,
                        cooldownMinutes = cooldownMinutes
                    )
                }
            }
        }
        onSave()
    }

    GlassBackground {
        Scaffold(containerColor = Color.Transparent) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GlassTheme.colors.textPrimary
                        )
                    }
                    Column {
                        Text(
                            text = "Add usage limit",
                            color = GlassTheme.colors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Create a rule that blocks apps by daily usage, by short sessions, or limits Shorts/Reels.",
                            color = GlassTheme.colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                InfoBanner(
                    title = "Scheduled blocks",
                    description = "Time-window blocking (e.g. 22:00–07:00) lives on the Scheduled Blocking screen."
                )

                SectionCard(title = "Target apps", subtitle = "Pick one or more apps for this rule.") {
                    if (selectedApp != null) {
                        Text("Starting from ${selectedApp.name}", color = GlassTheme.colors.textPrimary)
                    }
                    StatusChip(text = "${selectedAppIds.size} selected", isActive = selectedAppIds.isNotEmpty())
                    Button(
                        onClick = { showPicker = true },
                        enabled = availableApps.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlassTheme.colors.accentPrimary,
                            contentColor = GlassTheme.colors.textPrimary
                        )
                    ) {
                        Text("Choose apps")
                    }
                }

                SectionCard(title = "Rule type", subtitle = "Choose how the rule behaves.") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RuleType.entries.forEach { type ->
                            ToggleChip(text = type.label, selected = ruleType == type, onClick = { ruleType = type })
                        }
                    }
                }

                SectionCard(title = "Limit values", subtitle = null) {
                    if (ruleType != RuleType.SHORTS) {
                        OutlinedTextField(
                            value = limitName,
                            onValueChange = { limitName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Rule name") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = GlassTheme.colors.textPrimary,
                                unfocusedTextColor = GlassTheme.colors.textPrimary,
                                focusedBorderColor = GlassTheme.colors.accentPrimary,
                                unfocusedBorderColor = GlassTheme.colors.borderEnd,
                                focusedLabelColor = GlassTheme.colors.accentPrimary,
                                unfocusedLabelColor = GlassTheme.colors.textSecondary
                            )
                        )
                    }
                    when (ruleType) {
                        RuleType.APP_LIMIT -> {
                            LabeledSlider("Daily cap (all selected apps together)", dailyLimitMinutes, "m", 5..480, 5) {
                                dailyLimitMinutes = it
                            }
                            Text("When the cap is used up, the apps stay blocked until midnight.", color = GlassTheme.colors.textSecondary, fontSize = 12.sp)
                        }

                        RuleType.SESSIONS -> {
                            LabeledSlider("Session length (M)", sessionLengthMinutes, "m", 1..60, 1) { sessionLengthMinutes = it }
                            LabeledSlider("Sessions per day (N)", maxSessions, "", 1..30, 1) { maxSessions = it }
                            LabeledSlider("Cool-down between sessions (K)", cooldownMinutes, "m", 0..180, 5) { cooldownMinutes = it }
                        }

                        RuleType.SHORTS -> {
                            Text("Only Shorts/Reels are limited — the rest of the app keeps working.", color = GlassTheme.colors.textSecondary, fontSize = 12.sp)
                            ShortsModePicker(selected = shortsMode) { shortsMode = it }
                            when (shortsMode) {
                                ShortsMode.BLOCKED -> Text("Shorts/Reels are closed every time they open.", color = GlassTheme.colors.textPrimary)
                                ShortsMode.BUDGET -> {
                                    LabeledSlider("Daily Shorts/Reels budget", shortsBudgetMinutes, "m", 0..120, 5) { shortsBudgetMinutes = it }
                                    LabeledSlider("Full app block after budget", shortsPenaltyMinutes, "m", 5..240, 5) { shortsPenaltyMinutes = it }
                                }
                                ShortsMode.SESSIONS -> {
                                    LabeledSlider("Shorts session length (M)", sessionLengthMinutes, "m", 1..60, 1) { sessionLengthMinutes = it }
                                    LabeledSlider("Shorts sessions per day (N)", maxSessions, "", 1..30, 1) { maxSessions = it }
                                    LabeledSlider("Pause after a session (K)", cooldownMinutes, "m", 0..180, 5) { cooldownMinutes = it }
                                }
                            }
                            Text("Applies to the global Shorts/Reels rule; selected apps are added to it.", color = GlassTheme.colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                }

                SectionCard(title = "Summary", subtitle = null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Rule: ${ruleType.label}", color = GlassTheme.colors.textPrimary)
                        Text(
                            "Apps: ${selectedAppIds.joinToString { id -> availableApps.firstOrNull { it.id == id }?.name ?: id }}",
                            color = GlassTheme.colors.textSecondary
                        )
                        val detail = when (ruleType) {
                            RuleType.APP_LIMIT -> "Daily cap: ${dailyLimitMinutes}m, then blocked until midnight"
                            RuleType.SESSIONS -> "$maxSessions × ${sessionLengthMinutes}m, ${cooldownMinutes}m cool-down"
                            RuleType.SHORTS -> when (shortsMode) {
                                ShortsMode.BLOCKED -> "Shorts/Reels fully blocked"
                                ShortsMode.BUDGET -> "${shortsBudgetMinutes}m Shorts/day, then ${shortsPenaltyMinutes}m app block"
                                ShortsMode.SESSIONS -> "Shorts: $maxSessions × ${sessionLengthMinutes}m, ${cooldownMinutes}m pause"
                            }
                        }
                        Text(detail, color = GlassTheme.colors.textPrimary)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlassTheme.colors.surfaceElevated,
                            contentColor = GlassTheme.colors.textSecondary
                        )
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = ::save,
                        enabled = selectedAppIds.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlassTheme.colors.accentPrimary,
                            contentColor = GlassTheme.colors.textPrimary
                        )
                    ) {
                        Text("Save")
                    }
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

@Composable
private fun ColumnScope.LabeledSlider(
    label: String,
    value: Int,
    unit: String,
    range: IntRange,
    step: Int,
    onChange: (Int) -> Unit
) {
    Text("$label: $value$unit", color = GlassTheme.colors.textPrimary)
    Slider(
        value = value.toFloat(),
        onValueChange = { raw -> onChange(((raw / step).roundToInt() * step).coerceIn(range)) },
        valueRange = range.first.toFloat()..range.last.toFloat(),
        steps = ((range.last - range.first) / step - 1).coerceAtLeast(0),
        colors = SliderDefaults.colors(
            thumbColor = GlassTheme.colors.accentPrimary,
            activeTrackColor = GlassTheme.colors.accentPrimary,
            inactiveTrackColor = GlassTheme.colors.borderEnd
        )
    )
}
