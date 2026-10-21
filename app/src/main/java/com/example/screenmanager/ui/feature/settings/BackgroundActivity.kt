package com.example.screenmanager.ui.feature.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.screenmanager.ui.components.AppCard
import com.example.screenmanager.ui.components.CardDivider
import com.example.screenmanager.ui.components.IconBadge
import com.example.screenmanager.ui.components.ListRow
import com.example.screenmanager.ui.components.TextAction
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing

/**
 * Rad u pozadini mimo Android dozvola (BUG-018).
 *
 * Xiaomi/Redmi/POCO bez "Autostart" ubijaju proces i ne daju mu da se vrati,
 * iako su sve dozvole (i izuzetak od optimizacije baterije) date — limiti i
 * Shorts blokada tada stoje dok korisnik ne otvori aplikaciju. Stanje
 * "Autostart" se ne može pročitati, pa se nudi samo prečica do podešavanja.
 */
internal object BackgroundActivity {
    private val xiaomiBrands = setOf("xiaomi", "redmi", "poco")

    val isXiaomi: Boolean = listOf(Build.MANUFACTURER, Build.BRAND).any { it.orEmpty().lowercase() in xiaomiBrands }

    /** Xiaomi: lista "Autostart"; inače (ili ako je nema) informacije o aplikaciji → Baterija. */
    fun openAutostart(context: Context) {
        val autostart = Intent().setComponent(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
        )
        if (!isXiaomi || !context.tryStart(autostart)) openAppInfo(context)
    }

    fun openAppInfo(context: Context) {
        context.tryStart(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
    }

    private fun Context.tryStart(intent: Intent): Boolean =
        runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
}

/** Upozorenje kad je sistem ugasio Focus Flow u pozadini ([com.example.screenmanager.service.MonitorHealth]). */
@Composable
internal fun BackgroundStoppedCard(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val fix = if (BackgroundActivity.isXiaomi) {
        "Turn on Autostart for Focus Flow, then in App info → Battery saver choose \"No restrictions\"."
    } else {
        "In App info → Battery choose \"Unrestricted\" and allow background activity."
    }
    AppCard(contentPadding = PaddingValues(0.dp), borderColor = AppTheme.colors.warning) {
        ListRow(
            title = "Focus Flow was stopped in the background",
            subtitle = "Your phone closed Focus Flow, so limits and Shorts blocking were off until you opened the app. $fix",
            leading = { IconBadge(icon = Icons.Rounded.WarningAmber, tint = AppTheme.colors.warning) }
        )
        CardDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm),
            horizontalArrangement = Arrangement.End
        ) {
            TextAction(text = "Dismiss", onClick = onDismiss, color = AppTheme.colors.textSecondary)
            TextAction(
                text = if (BackgroundActivity.isXiaomi) "Open Autostart" else "Open App info",
                onClick = { BackgroundActivity.openAutostart(context) }
            )
        }
    }
}

/** Red u "Permissions" samo na Xiaomi telefonima: prečice do Autostart-a i štednje baterije. */
@Composable
internal fun XiaomiBackgroundRows() {
    val context = LocalContext.current
    ListRow(
        title = "Autostart (Xiaomi)",
        subtitle = "Lets Focus Flow restart after the phone closes it. Without it limits stop working " +
            "in the background. Also lock Focus Flow in recent apps.",
        onClick = { BackgroundActivity.openAutostart(context) },
        leading = { IconBadge(icon = Icons.Rounded.Autorenew, tint = AppTheme.colors.textSecondary) },
        trailing = { TextAction(text = "Open", onClick = { BackgroundActivity.openAutostart(context) }) }
    )
    CardDivider()
    ListRow(
        title = "Battery saver (Xiaomi)",
        subtitle = "App info → Battery saver → \"No restrictions\".",
        onClick = { BackgroundActivity.openAppInfo(context) },
        leading = { IconBadge(icon = Icons.Rounded.Autorenew, tint = AppTheme.colors.textSecondary) },
        trailing = { TextAction(text = "Open", onClick = { BackgroundActivity.openAppInfo(context) }) }
    )
}
