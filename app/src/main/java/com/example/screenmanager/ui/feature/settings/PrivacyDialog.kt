package com.example.screenmanager.ui.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.screenmanager.ui.components.TextAction
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing

/** Javna politika privatnosti (docs/PRIVACY.md preko GitHub Pages); isti URL ide u Play Console. */
const val PRIVACY_POLICY_URL = "https://vidanstojic.github.io/Screen-Manager/PRIVACY"

/** Sažetak politike privatnosti u aplikaciji; ceo tekst je na [PRIVACY_POLICY_URL]. */
@Composable
fun PrivacyDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.colors.surface,
        title = {
            Text(text = "Privacy", style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.textPrimary)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                PrivacySection(
                    "Stored on this device",
                    "App usage times from Android's usage statistics, your rules, limits and alarms, " +
                        "and Shorts/Reels watch-time totals."
                )
                PrivacySection(
                    "Accessibility service",
                    "Reads the screen layout of YouTube and Instagram to detect Shorts and Reels, and " +
                        "the browser address bar only during a wake-up detox with blocked sites. " +
                        "Screen content is checked on the fly and never saved."
                )
                PrivacySection(
                    "Not collected or shared",
                    "No account, no analytics, no ads, no internet access. Data is excluded from " +
                        "cloud backup and device transfer, and is deleted when you uninstall the app."
                )
            }
        },
        confirmButton = { TextAction(text = "Close", onClick = onDismiss) },
        dismissButton = {
            TextAction(
                text = "Full policy",
                onClick = { context.openUrl(PRIVACY_POLICY_URL) },
                color = AppTheme.colors.textSecondary
            )
        }
    )
}

@Composable
private fun PrivacySection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.colors.textPrimary
        )
        Text(text = body, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textSecondary)
    }
}

private fun Context.openUrl(url: String) {
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
