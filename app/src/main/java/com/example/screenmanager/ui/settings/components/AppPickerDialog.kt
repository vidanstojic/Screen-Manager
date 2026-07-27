package com.example.screenmanager.ui.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Pomoćni model za prikaz u listi
data class AppInfo(val id: String, val name: String)

@Composable
fun AppPickerDialog(
    availableApps: List<AppInfo>,
    initialSelectedIds: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    // Čuvamo lokalno stanje selekcije dok korisnik ne potvrdi
    var selectedIds by remember { mutableStateOf(initialSelectedIds.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Izaberi aplikacije") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(availableApps) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedIds = if (selectedIds.contains(app.id)) {
                                    selectedIds - app.id
                                } else {
                                    selectedIds + app.id
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedIds.contains(app.id),
                            onCheckedChange = null // Handle-ujemo preko Row klika
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = app.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedIds.toList()) }) {
                Text("Sačuvaj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Otkaži")
            }
        }
    )
}