package com.example.screenmanager.ui.limits

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.screenmanager.ScreenManagerApplication
import com.example.screenmanager.ui.settings.SettingsViewModel
import com.example.screenmanager.ui.settings.SettingsViewModelFactory
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.screenmanager.domain.getInstalledApps
import com.example.screenmanager.model.ScheduleRule
import com.example.screenmanager.ui.settings.components.ScheduledBlockSection
import com.example.screenmanager.ui.theme.GlassBackground
import com.example.screenmanager.ui.theme.GlassTheme

/**
 * Poseban ekran za upravljanje vremenskim blok pravilima.
 *
 * Do njega se dolazi iz [UsageLimitsScreen] i ovde korisnik pravi nova
 * scheduled pravila kroz lokalni dialog.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledBlockScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(ScreenManagerApplication.getInstance().settingsRepository)
    )
) {
    val context = LocalContext.current

    val realApps = remember { getInstalledApps(context) }

    // Pravila dolaze iz baze — ranije su živela samo u lokalnom state-u i
    // nestajala izlaskom sa ekrana (a servis ih nikad nije video).
    val rules by viewModel.scheduleRules.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddScheduleDialog(
            availableApps = realApps,
            onDismiss = { showAddDialog = false },
            onSaveRule = { newRule ->
                viewModel.saveScheduleRule(newRule)
                showAddDialog = false
            }
        )
    }

    GlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Scheduled Blocking", fontWeight = FontWeight.Bold, color = GlassTheme.colors.textPrimary) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = GlassTheme.colors.textPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = GlassTheme.colors.accentPrimary,
                    contentColor = GlassTheme.colors.textPrimary,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Schedule")
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                ScheduledBlockSection(
                    rules = rules,
                    onToggleRule = { ruleId, isEnabled -> viewModel.toggleScheduleRule(ruleId, isEnabled) },
                    onAddScheduleClick = { showAddDialog = true }
                )
            }
        }
    }
}