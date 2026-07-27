package com.example.screenmanager.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.screenmanager.model.MainDestination // 👉 Dodat import
// import com.example.screenmanager.ui.components.SharedBottomBar // 👉 Otkomentariši/prilagodi naziv tvoje komponente za meni
import com.example.screenmanager.ui.settings.components.AppPickerDialog
import com.example.screenmanager.ui.settings.components.*

// Enum da znamo za koga otvaramo App Picker
enum class AppPickerTarget {
    NONE, WAKE_UP, SHORT_VIDEO
}

@Composable
fun GeneralSettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    destination: MainDestination,                     // 👉 Dodat parametar
    onDestinationSelected: (MainDestination) -> Unit  // 👉 Dodat parametar
) {
    val wakeUpConfig by viewModel.wakeUpConfig.collectAsState()
    val shortVideoConfig by viewModel.shortVideoConfig.collectAsState()
    val scheduleRules by viewModel.scheduleRules.collectAsState()

    // Upravljanje dijalogom
    var activeDialogTarget by remember { mutableStateOf(AppPickerTarget.NONE) }

    // Mock lista svih aplikacija (u produkciji dobijaš preko PackageManager-a)
    val allAppsMock = remember {
        listOf(
            AppInfo("instagram", "Instagram"),
            AppInfo("youtube", "YouTube"),
            AppInfo("tiktok", "TikTok"),
            AppInfo("facebook", "Facebook"),
            AppInfo("reddit", "Reddit")
        )
    }

    Scaffold(
        // 👉 Dodajemo bottomBar kako bi meni bio vidljiv i na ovom ekranu
        bottomBar = {
            // Zameni 'SharedBottomBar' imenom tvoje stvarne komponente za donji meni
            /*
            SharedBottomBar(
                selectedDestination = destination,
                onDestinationSelected = onDestinationSelected
            )
            */
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                WakeUpSection(
                    config = wakeUpConfig,
                    onConfigChange = { viewModel.updateWakeUpConfig(it) },
                    onSelectAppsClick = { activeDialogTarget = AppPickerTarget.WAKE_UP }
                )
            }

            item {
                ShortVideoSection(
                    config = shortVideoConfig,
                    onConfigChange = { viewModel.updateShortVideoConfig(it) },
                    onSelectAppsClick = { activeDialogTarget = AppPickerTarget.SHORT_VIDEO }
                )
            }

            item {
                ScheduledBlockSection(
                    rules = scheduleRules,
                    onToggleRule = { id, enabled -> viewModel.toggleScheduleRule(id, enabled) },
                    onAddScheduleClick = { viewModel.addMockScheduleRule() }
                )
            }
        }

        // Prikaz univerzalnog AppPicker dijaloga
        if (activeDialogTarget != AppPickerTarget.NONE) {
            val initialIds = when (activeDialogTarget) {
                AppPickerTarget.WAKE_UP -> wakeUpConfig.selectedAppIds
                AppPickerTarget.SHORT_VIDEO -> shortVideoConfig.selectedAppIds
                else -> emptyList()
            }

            AppPickerDialog(
                availableApps = allAppsMock,
                initialSelectedIds = initialIds,
                onDismiss = { activeDialogTarget = AppPickerTarget.NONE },
                onConfirm = { selectedApps ->
                    when (activeDialogTarget) {
                        AppPickerTarget.WAKE_UP -> {
                            viewModel.updateWakeUpConfig(wakeUpConfig.copy(selectedAppIds = selectedApps))
                        }
                        AppPickerTarget.SHORT_VIDEO -> {
                            viewModel.updateShortVideoConfig(shortVideoConfig.copy(selectedAppIds = selectedApps))
                        }
                        else -> {}
                    }
                    activeDialogTarget = AppPickerTarget.NONE
                }
            )
        }
    }
}