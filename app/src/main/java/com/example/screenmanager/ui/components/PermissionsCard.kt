package com.example.screenmanager.ui.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.screenmanager.domain.PermissionState
import com.example.screenmanager.domain.PermissionStateChecker
import com.example.screenmanager.ui.theme.GlassTheme

/**
 * Onboarding za kritične dozvole (TRS §3). Bez Usage Access-a nema
 * statistike ni monitoring servisa, bez overlay-a nema blok ekrana, bez
 * accessibility-ja nema Shorts/Reels blokera.
 */
@Composable
fun PermissionsCard(state: PermissionState, modifier: Modifier = Modifier) {
    if (state.allCriticalGranted) return
    val context = LocalContext.current

    SectionCard(
        title = "Potrebne dozvole",
        subtitle = "Bez ovih dozvola aplikacija ne može da prati vreme niti da blokira aplikacije.",
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PermissionRow("Usage access (statistika)", state.hasUsageAccess) {
                context.openSettings(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
            PermissionRow("Prikaz preko drugih aplikacija (blok ekran)", state.canDrawOverlay) {
                context.openSettings(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, PermissionStateChecker.overlaySettingsUri(context))
                )
            }
            PermissionRow("Accessibility (Shorts/Reels)", state.accessibilityEnabled) {
                context.openSettings(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onGrant: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (granted) GlassTheme.colors.textSecondary else GlassTheme.colors.textPrimary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        if (granted) {
            Text("✓", color = GlassTheme.colors.success)
        } else {
            TextButton(onClick = onGrant) {
                Text("Dozvoli", color = GlassTheme.colors.accentPrimary)
            }
        }
    }
}

private fun Context.openSettings(intent: Intent) {
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
