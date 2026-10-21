package com.example.screenmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.screenmanager.model.AppOption
import com.example.screenmanager.ui.common.pluralize
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.AuroraBackground
import com.example.screenmanager.ui.theme.Spacing

/**
 * Polje forme koje prikazuje izabrane aplikacije (ikonice + imena) i
 * otvara [AppPickerDialog] na klik.
 */
@Composable
fun AppSelectionField(
    selectedIds: List<String>,
    labelFor: (String) -> String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectedIds.isEmpty()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Choose apps",
                    style = MaterialTheme.typography.titleSmall,
                    color = AppTheme.colors.accent
                )
                Text(
                    text = "No apps selected yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary
                )
            }
        } else {
            AppIconStack(packageNames = selectedIds.take(MAX_STACKED_ICONS), labelFor = labelFor)
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pluralize(selectedIds.size, "app"),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppTheme.colors.textPrimary
                )
                Text(
                    text = selectedIds.joinToString { labelFor(it) },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(Spacing.sm))
        ChevronIcon()
    }
}

private const val MAX_STACKED_ICONS = 4

/**
 * Izbor aplikacija preko celog ekrana: pretraga + lista sa potvrdom.
 * Izbor se vraća tek na "Done" ([onConfirm]); "X" odbacuje izmene.
 */
@Composable
fun AppPickerDialog(
    availableApps: List<AppOption>,
    initialSelectedIds: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(initialSelectedIds.toSet()) }

    val filteredApps = remember(query, availableApps) {
        if (query.isBlank()) availableApps
        else availableApps.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }

    // Visina donje trake sa dugmetom (zavisi od navigacione trake/tastature) — toliko
    // praznog prostora dobija kraj liste, da poslednja aplikacija ne ostane ispod dugmeta.
    var confirmBarHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Dialog(
        onDismissRequest = onDismiss,
        // decorFitsSystemWindows = false: prozor dijaloga ide preko sistemskih traka i Compose
        // dobija prave insets-e. Sa podrazumevanim true (targetSdk 35+ je uvek edge-to-edge)
        // insets-i su bili 0, pa je dugme "Done" padalo ispod navigacione trake.
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        // Dijalog je zaseban prozor, pa crta svoju aurora pozadinu.
        AuroraBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleIconButton(icon = Icons.Rounded.Close, contentDescription = "Close", onClick = onDismiss)
                    Spacer(Modifier.width(Spacing.md))
                    Text(
                        text = "Choose apps",
                        style = MaterialTheme.typography.titleLarge,
                        color = AppTheme.colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }

                AppTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = "Search",
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = Spacing.screen)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = Spacing.sm,
                            bottom = with(density) { confirmBarHeightPx.toDp() } + Spacing.sm
                        )
                    ) {
                        items(filteredApps, key = { it.id }) { app ->
                            val isSelected = app.id in selectedIds
                            AppPickerRow(
                                app = app,
                                selected = isSelected,
                                onToggle = {
                                    selectedIds = if (isSelected) selectedIds - app.id else selectedIds + app.id
                                }
                            )
                        }
                        if (filteredApps.isEmpty()) {
                            item {
                                Text(
                                    text = if (availableApps.isEmpty()) "Loading apps…" else "No apps match \"$query\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppTheme.colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.xl)
                                )
                            }
                        }
                    }

                    PickerConfirmBar(
                        text = if (selectedIds.isEmpty()) "Done" else "Done · ${selectedIds.size} selected",
                        onConfirm = { onConfirm(selectedIds.toList()) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .onSizeChanged { confirmBarHeightPx = it.height }
                    )
                }
            }
        }
    }
}

/**
 * Dugme za potvrdu koje lebdi nad listom, na prozirnom prelazu u boju pozadine.
 *
 * Razmak od dna prati telefon: sa navigacionom trakom (dugmad ili gesture linija)
 * dugme stoji [Spacing.md] iznad nje, bez trake [MIN_BOTTOM_GAP] od ivice ekrana;
 * dok je tastatura otvorena (pretraga), stoji iznad tastature.
 */
@Composable
private fun PickerConfirmBar(text: String, onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val systemBottom = with(density) { WindowInsets.navigationBars.union(WindowInsets.ime).getBottom(density).toDp() }
    val bottomGap = (systemBottom + Spacing.md).coerceAtLeast(MIN_BOTTOM_GAP)
    val background = AppTheme.colors.background
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.4f to background.copy(alpha = 0.85f),
                    1f to background
                )
            )
            .padding(start = Spacing.screen, end = Spacing.screen, top = Spacing.xl, bottom = bottomGap)
    ) {
        PrimaryButton(text = text, onClick = onConfirm, modifier = Modifier.fillMaxWidth())
    }
}

private val MIN_BOTTOM_GAP = Spacing.xl

@Composable
private fun AppPickerRow(app: AppOption, selected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = Spacing.screen, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        AppIcon(packageName = app.id, label = app.name)
        Text(
            text = app.name,
            style = MaterialTheme.typography.titleSmall,
            color = AppTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (selected) AppTheme.colors.accentStart else AppTheme.colors.surfaceRaised),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = AppTheme.colors.onAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
