package com.example.screenmanager.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.screenmanager.ScreenManagerApplication
import com.example.screenmanager.model.AppLimitRule
import com.example.screenmanager.model.AppOption
import com.example.screenmanager.model.EmergencySessionConfig
import com.example.screenmanager.model.MainDestination
import com.example.screenmanager.model.ShortVideoConfig
import com.example.screenmanager.model.WakeUpConfig
import com.example.screenmanager.ui.components.BottomNavBar
import com.example.screenmanager.ui.components.InfoBanner
import com.example.screenmanager.ui.components.SectionCard
import com.example.screenmanager.ui.settings.components.AppLimitRulesSection
import com.example.screenmanager.ui.settings.components.AppPickerDialog
import com.example.screenmanager.ui.settings.components.EmergencySessionSection
import com.example.screenmanager.ui.settings.components.ScheduledBlockSection
import com.example.screenmanager.ui.settings.components.ShortVideoSection
import com.example.screenmanager.ui.settings.components.WakeUpSection

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

    var activeDialogTarget by remember { mutableStateOf(AppPickerTarget.NONE) }
    var editingAppLimitRule by remember { mutableStateOf<AppLimitRule?>(null) }

    Scaffold(
        bottomBar = {
            BottomNavBar(
                selected = destination,
                onSelected = onDestinationSelected
            )
        }
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
                    onActivateSession = {
                        val cfg = emergencySession ?: EmergencySessionConfig()
                        val next = cfg.copy(
                            isActive = true,
                            activeUntilLabel = "${cfg.defaultDurationMinutes}m from now"
                        )
                        viewModel.updateEmergencySession(next)
                    },
                    onEndSession = {
                        val cfg = emergencySession ?: EmergencySessionConfig()
                        viewModel.updateEmergencySession(
                            cfg.copy(isActive = false, activeUntilLabel = null)
                        )
                    }
                )
            }

            item {
                SectionCard(
                    title = "Available apps",
                    subtitle = "These are the app targets the UI is prepared to manage."
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableApps.forEach { app ->
                            AppOptionRow(app = app)
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
                else -> emptyList()
            }

            AppPickerDialog(
                availableApps = availableApps,
                initialSelectedIds = initialIds,
                onDismiss = {
                    activeDialogTarget = AppPickerTarget.NONE
                    editingAppLimitRule = null
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
                        else -> Unit
                    }
                    activeDialogTarget = AppPickerTarget.NONE
                    editingAppLimitRule = null
                }
            )
        }
    }
}

@Composable
private fun AppOptionRow(app: AppOption) {
    Column {
        androidx.compose.material3.Text(text = app.name)
        androidx.compose.material3.Text(text = "${app.category} • ${app.id}")
    }
}

private enum class AppPickerTarget {
    NONE, WAKE_UP, SHORT_VIDEO, APP_LIMIT
}
