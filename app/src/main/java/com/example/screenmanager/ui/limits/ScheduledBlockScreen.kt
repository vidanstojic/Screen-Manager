package com.example.screenmanager.ui.limits

import androidx.compose.foundation.layout.*
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
import com.example.screenmanager.model.ScheduleRule
import com.example.screenmanager.ui.settings.components.ScheduledBlockSection
import com.example.screenmanager.ui.theme.DetailBackground
import com.example.screenmanager.ui.theme.PurpleAccent
import com.example.screenmanager.domain.getInstalledApps

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledBlockScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Učitavamo stvarne aplikacije sa uređaja/emulatora
    val realApps = remember { getInstalledApps(context) }

    var rules by remember { mutableStateOf<List<ScheduleRule>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddScheduleDialog(
            availableApps = realApps,
            onDismiss = { showAddDialog = false },
            onSaveRule = { newRule ->
                rules = rules + newRule
                showAddDialog = false
            }
        )
    }

    Scaffold(
        containerColor = DetailBackground,
        topBar = {
            TopAppBar(
                title = { Text("Scheduled Blocking", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DetailBackground,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PurpleAccent,
                contentColor = Color(0xFF1F1B29)
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
                onToggleRule = { ruleId, isEnabled ->
                    rules = rules.map {
                        if (it.id == ruleId) it.copy(isEnabled = isEnabled) else it
                    }
                },
                onAddScheduleClick = { showAddDialog = true }
            )
        }
    }
}