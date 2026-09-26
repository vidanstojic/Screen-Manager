package com.example.screenmanager.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.screenmanager.ScreenManagerApplication
import com.example.screenmanager.domain.getInstalledApps
import com.example.screenmanager.model.AppLimitRule
import com.example.screenmanager.model.AppOption
import com.example.screenmanager.model.EmergencySessionConfig
import com.example.screenmanager.model.MainDestination
import com.example.screenmanager.model.SessionLimitRule
import com.example.screenmanager.model.ShortVideoConfig
import com.example.screenmanager.model.WakeUpConfig
import com.example.screenmanager.ui.components.InfoBanner
import com.example.screenmanager.ui.components.SectionCard
import com.example.screenmanager.ui.settings.components.AppLimitRulesSection
import com.example.screenmanager.ui.settings.components.AppPickerDialog
import com.example.screenmanager.ui.settings.components.EmergencySessionSection
import com.example.screenmanager.ui.settings.components.ScheduledBlockSection
import com.example.screenmanager.ui.settings.components.SessionLimitRulesSection
import com.example.screenmanager.ui.settings.components.ShortVideoSection
import com.example.screenmanager.ui.settings.components.WakeUpSection
import com.example.screenmanager.ui.theme.GlassBackground
import com.example.screenmanager.ui.theme.GlassTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Glavni settings ekran za zaštitna pravila aplikacije.
 */
@Composable
fun GeneralSettingsScreen(
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(ScreenManagerApplication.getInstance().settingsRepository)
    ),
    destination: MainDestination,
    onDestinationSelected: (MainDestination) -> Unit
) {
    val wakeUpConfig by viewModel.wakeUpConfig.collectAsState()
    val shortVideoConfig by viewModel.shortVideoConfig.collectAsState()
    val scheduleRules by viewModel.scheduleRules.collectAsState()
    val appLimitRules by viewModel.appLimitRules.collectAsState()
    val emergencySession by viewModel.emergencySession.collectAsState()
    val availableApps by viewModel.availableApps.collectAsState()
    val sessionLimitRules by viewModel.sessionLimitRules.collectAsState()

    var activeDialogTarget by remember { mutableStateOf(AppPickerTarget.NONE) }
    var editingAppLimitRule by remember { mutableStateOf<AppLimitRule?>(null) }
    var editingSessionRule by remember { mutableStateOf<SessionLimitRule?>(null) }

    // Picker nudi stvarno instalirane aplikacije (učitavanje van main thread-a).
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val installed = withContext(Dispatchers.IO) { getInstalledApps(context) }
        if (installed.isNotEmpty()) viewModel.replaceAvailableApps(installed)
    }

    GlassBackground {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    InfoBanner(
                        title = "Protection controls",
                        description = "Configure app limits, Shorts/Reels blocking, scheduled blocks, and wake-up blocking from one place."
                    )
                }

                item {
                    AppLimitRulesSection(
                        rules = appLimitRules,
                        onToggleRule = { id, enabled -> viewModel.toggleAppLimitRule(id, enabled) },
                        onAddRule = { viewModel.addAppLimitRule() },
                        onEditRule = { rule ->
                            editingAppLimitRule = rule
                            activeDialogTarget = AppPickerTarget.APP_LIMIT
                        },
                        onRemoveRule = { viewModel.removeAppLimitRule(it) }
                    )
                }

                item {
                    SessionLimitRulesSection(
                        rules = sessionLimitRules,
                        onRuleChange = { viewModel.upsertSessionLimitRule(it) },
                        onAddRule = { viewModel.addSessionLimitRule() },
                        onEditApps = { rule ->
                            editingSessionRule = rule
                            activeDialogTarget = AppPickerTarget.SESSION_LIMIT
                        },
                        onRemoveRule = { viewModel.removeSessionLimitRule(it) }
                    )
                }

                item {
                    ShortVideoSection(
                        config = shortVideoConfig ?: ShortVideoConfig(),
                        onConfigChange = { viewModel.updateShortVideoConfig(it) },
                        onSelectAppsClick = { activeDialogTarget = AppPickerTarget.SHORT_VIDEO }
                    )
                }

                item {
                    WakeUpSection(
                        config = wakeUpConfig ?: WakeUpConfig(),
                        onConfigChange = { viewModel.updateWakeUpConfig(it) },
                        onSelectAppsClick = { activeDialogTarget = AppPickerTarget.WAKE_UP }
                    )
                }

                item {
                    ScheduledBlockSection(
                        rules = scheduleRules,
                        onToggleRule = { id, enabled -> viewModel.toggleScheduleRule(id, enabled) },
                        onAddScheduleClick = { viewModel.addScheduleRule() }
                    )
                }

                item {
                    EmergencySessionSection(
                        config = emergencySession ?: EmergencySessionConfig(),
                        onConfigChange = { viewModel.updateEmergencySession(it) },
                        onActivateSession = { viewModel.activateEmergencySession() },
                        onEndSession = { viewModel.endEmergencySession() }
                    )
                }

                item {
                    SectionCard(
                        title = "Available apps",
                        subtitle = "These are the app targets the UI is prepared to manage."
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            availableApps.take(MAX_APPS_PREVIEW).forEach { app ->
                                AppOptionRow(app = app)
                            }
                            if (availableApps.size > MAX_APPS_PREVIEW) {
                                Text(
                                    text = "+ ${availableApps.size - MAX_APPS_PREVIEW} more (use \"Apps\" on a rule to pick)",
                                    color = GlassTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            if (activeDialogTarget != AppPickerTarget.NONE) {
                val initialIds = when (activeDialogTarget) {
                    AppPickerTarget.WAKE_UP -> (wakeUpConfig ?: WakeUpConfig()).selectedAppIds
                    AppPickerTarget.SHORT_VIDEO -> (shortVideoConfig ?: ShortVideoConfig()).selectedAppIds
                    AppPickerTarget.APP_LIMIT -> editingAppLimitRule?.selectedAppIds ?: emptyList()
                    AppPickerTarget.SESSION_LIMIT -> editingSessionRule?.selectedAppIds ?: emptyList()
                    else -> emptyList()
                }

                AppPickerDialog(
                    availableApps = availableApps,
                    initialSelectedIds = initialIds,
                    onDismiss = {
                        activeDialogTarget = AppPickerTarget.NONE
                        editingAppLimitRule = null
                        editingSessionRule = null
                    },
                    onConfirm = { selectedApps ->
                        when (activeDialogTarget) {
                            AppPickerTarget.WAKE_UP -> {
                                (wakeUpConfig ?: WakeUpConfig()).let { cfg ->
                                    viewModel.updateWakeUpConfig(cfg.copy(selectedAppIds = selectedApps))
                                }
                            }
                            AppPickerTarget.SHORT_VIDEO -> {
                                (shortVideoConfig ?: ShortVideoConfig()).let { cfg ->
                                    viewModel.updateShortVideoConfig(cfg.copy(selectedAppIds = selectedApps))
                                }
                            }
                            AppPickerTarget.APP_LIMIT -> {
                                editingAppLimitRule?.let { rule ->
                                    viewModel.updateAppLimitRule(rule.copy(selectedAppIds = selectedApps))
                                }
                            }
                            AppPickerTarget.SESSION_LIMIT -> {
                                editingSessionRule?.let { rule ->
                                    viewModel.upsertSessionLimitRule(rule.copy(selectedAppIds = selectedApps))
                                }
                            }
                            else -> Unit
                        }
                        activeDialogTarget = AppPickerTarget.NONE
                        editingAppLimitRule = null
                        editingSessionRule = null
                    }
                )
            }
        }
    }
}

@Composable
private fun AppOptionRow(app: AppOption) {
    Column {
        Text(text = app.name, color = GlassTheme.colors.textPrimary)
        Text(text = "${app.category} • ${app.id}", color = GlassTheme.colors.textSecondary)
    }
}

private const val MAX_APPS_PREVIEW = 8

private enum class AppPickerTarget {
    NONE, WAKE_UP, SHORT_VIDEO, APP_LIMIT, SESSION_LIMIT
}