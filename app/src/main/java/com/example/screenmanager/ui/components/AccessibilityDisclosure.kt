package com.example.screenmanager.ui.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.screenmanager.domain.ServiceLocator
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing

/**
 * Obavezno obaveštenje pre uključivanja Accessibility servisa (Google Play
 * "prominent disclosure"): šta se čita, zašto i da podaci ne napuštaju uređaj.
 * Mora se prikazati pre svakog slanja korisnika u sistemska podešavanja, a
 * u podešavanja se ide tek na "Agree".
 */
@Composable
fun AccessibilityDisclosureDialog(onAgree: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.colors.surface,
        title = {
            Text(
                text = "Allow Accessibility access?",
                style = MaterialTheme.typography.titleMedium,
                color = AppTheme.colors.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                DisclosureParagraph(
                    "Focus Flow uses the Android Accessibility service to enforce limits you set " +
                        "yourself. It is not used as an assistive tool."
                )
                DisclosureSection(
                    "What it reads",
                    "• YouTube and Instagram: the screen layout (element IDs and labels), to recognize " +
                        "when the Shorts or Reels player is open.\n" +
                        "• Web browsers, only while a wake-up detox with blocked sites is running: the " +
                        "address bar, to check whether a blocked site is open."
                )
                DisclosureSection(
                    "What it does",
                    "Leaves Shorts/Reels or the blocked site and shows a block screen when one of " +
                        "your limits applies. It never types, taps or reads anything else."
                )
                DisclosureSection(
                    "Your data",
                    "Everything is processed on this device. Only Shorts/Reels watch-time totals are " +
                        "saved. Nothing is collected, sent or shared with anyone."
                )
                DisclosureParagraph("You can turn it off at any time in Settings › Accessibility.")
            }
        },
        confirmButton = { TextAction(text = "Agree", onClick = onAgree) },
        dismissButton = { TextAction(text = "Not now", onClick = onDismiss, color = AppTheme.colors.textSecondary) }
    )
}

/**
 * Uključivanje funkcije kojoj treba Accessibility servis (Shorts/Reels): Play traži da se
 * obaveštenje pojavi u normalnom toku, ne samo iz Settings-a (REL-002).
 */
class AccessibilityGate internal constructor(private val request: (action: () -> Unit) -> Unit) {
    /** Ako [needsService] a servis nije uključen, prvo obaveštenje; [action] se izvršava u oba slučaja. */
    fun run(needsService: Boolean, action: () -> Unit) {
        if (needsService) request(action) else action()
    }
}

/**
 * Servis uključen → akcija odmah. Inače [AccessibilityDisclosureDialog]: "Agree" izvršava
 * akciju i otvara sistemska podešavanja, "Not now" samo izvršava akciju (pravilo je
 * sačuvano, ali se ne sprovodi dok se servis ne uključi).
 */
@Composable
fun rememberAccessibilityGate(): AccessibilityGate {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    pending?.let { action ->
        AccessibilityDisclosureDialog(
            onAgree = {
                pending = null
                context.openAccessibilitySettings()
                action()
            },
            onDismiss = {
                pending = null
                action()
            }
        )
    }
    return remember(context) {
        AccessibilityGate { action ->
            if (ServiceLocator.permissionStateChecker(context).isAccessibilityEnabled()) action() else pending = action
        }
    }
}

/** Sistemska Accessibility podešavanja; pozivati tek posle [AccessibilityDisclosureDialog]. */
fun Context.openAccessibilitySettings() {
    runCatching {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

@Composable
private fun DisclosureSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.colors.textPrimary
        )
        DisclosureParagraph(body)
    }
}

@Composable
private fun DisclosureParagraph(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textSecondary)
}
