package com.example.screenmanager.ui.feature.limits

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.screenmanager.domain.rules.LimitsLock
import com.example.screenmanager.ui.common.formatClock
import com.example.screenmanager.ui.common.pluralize
import com.example.screenmanager.ui.common.relativeDayLabel
import com.example.screenmanager.ui.common.rememberNow
import com.example.screenmanager.ui.components.AppCard
import com.example.screenmanager.ui.components.CardDivider
import com.example.screenmanager.ui.components.IconBadge
import com.example.screenmanager.ui.components.ListRow
import com.example.screenmanager.ui.components.SecondaryButton
import com.example.screenmanager.ui.components.StepperRow
import com.example.screenmanager.ui.components.TextAction
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Zaključavanje u formi sačuvanog pravila: [lockedUntil] = pravilo je zaključano (samo ono
 * ili sva pravila), [onLock] = zaključaj baš ovo pravilo. Za novo pravilo oba su `null`.
 * Postavlja ga RuleEditorRoute, čita RuleEditorScaffold.
 */
class RuleLockUi(val lockedUntil: Long?, val onLock: ((durationMs: Long) -> Unit)?)

val LocalRuleLock = compositionLocalOf { RuleLockUi(lockedUntil = null, onLock = null) }

/** Trenutno vreme za prikaz zaključavanja (osvežava se dok je ekran otvoren). */
@Composable
fun rememberLockNow(): Long {
    val now by rememberNow(intervalMs = 15_000L)
    return now
}

private val DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.ENGLISH)

/** "today 18:30", "Friday 09:00" ili "24 Nov, 09:00" za dalje datume. */
fun formatLockEnd(endsAt: Long, now: Long = System.currentTimeMillis()): String =
    if (endsAt - now < 6 * LimitsLock.DAY_MS) {
        val day = relativeDayLabel(endsAt, now).let { if (it == "Today" || it == "Tomorrow") it.lowercase() else it }
        "$day ${formatClock(endsAt)}"
    } else {
        DATE_TIME.format(Instant.ofEpochMilli(endsAt).atZone(ZoneId.systemDefault()))
    }

/** "2 days 5 hours", "3 hours", "1 day". */
private fun formatLockDuration(days: Int, hours: Int): String = listOfNotNull(
    days.takeIf { it > 0 }?.let { pluralize(it, "day") },
    hours.takeIf { it > 0 }?.let { pluralize(it, "hour") }
).joinToString(" ")

/**
 * Izbor trajanja (dani + sati) i potvrda; zaključavanje ne može da se poništi, samo produži.
 *
 * @param what šta se zaključava, za tekst: "all limits" / "this limit"
 */
@Composable
fun LockDurationDialog(what: String, onLock: (durationMs: Long) -> Unit, onDismiss: () -> Unit) {
    var days by rememberSaveable { mutableIntStateOf(1) }
    var hours by rememberSaveable { mutableIntStateOf(0) }
    val durationMs = days * LimitsLock.DAY_MS + hours * LimitsLock.HOUR_MS
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.colors.surface,
        title = { Text("Lock $what", color = AppTheme.colors.textPrimary) },
        text = {
            Column {
                StepperRow(label = "Days", value = days, onValueChange = { days = it }, range = 0..MAX_DAYS)
                StepperRow(label = "Hours", value = hours, onValueChange = { hours = it }, range = 0..23)
                Text(
                    text = if (durationMs > 0) {
                        "Until ${formatLockEnd(System.currentTimeMillis() + durationMs)} you won't be able to turn off, " +
                            "edit or delete $what, and the emergency pause won't be available. " +
                            "You can still add new limits. The lock can't be undone, only extended."
                    } else {
                        "Choose how long the lock lasts."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = Spacing.md)
                )
            }
        },
        confirmButton = {
            TextAction(
                text = if (durationMs > 0) "Lock for ${formatLockDuration(days, hours)}" else "Lock",
                enabled = durationMs > 0,
                onClick = { onLock(durationMs) }
            )
        },
        dismissButton = { TextAction(text = "Cancel", color = AppTheme.colors.textSecondary, onClick = onDismiss) }
    )
}

/** Opšte zaključavanje (sva pravila) na Limits ekranu. */
@Composable
fun LockAllLimitsCard(lockedUntil: Long?, onLock: (Long) -> Unit) {
    var choosing by rememberSaveable { mutableStateOf(false) }
    if (choosing) {
        LockDurationDialog(
            what = "all limits",
            onLock = {
                choosing = false
                onLock(it)
            },
            onDismiss = { choosing = false }
        )
    }
    AppCard(contentPadding = PaddingValues(0.dp)) {
        ListRow(
            title = if (lockedUntil != null) "All limits are locked" else "Lock all limits",
            subtitle = if (lockedUntil != null) {
                "Until ${formatLockEnd(lockedUntil)}. Limits can't be turned off, edited or deleted until then; " +
                    "you can still add new ones."
            } else {
                "Keep yourself from turning limits off on impulse. You can also lock a single limit from its own screen."
            },
            leading = {
                IconBadge(
                    icon = if (lockedUntil != null) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                    tint = if (lockedUntil != null) AppTheme.colors.accent else AppTheme.colors.textSecondary
                )
            }
        )
        CardDivider()
        SecondaryButton(
            text = if (lockedUntil != null) "Extend the lock" else "Choose how long",
            icon = Icons.Rounded.Lock,
            onClick = { choosing = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        )
    }
}

/** Na vrhu forme zaključanog pravila (Save i brisanje su tada isključeni). */
@Composable
fun LockedRuleBanner(lockedUntil: Long) {
    AppCard(contentPadding = PaddingValues(0.dp), borderColor = AppTheme.colors.accent) {
        ListRow(
            title = "This limit is locked",
            subtitle = "Until ${formatLockEnd(lockedUntil)}. It can't be turned off, changed or deleted until then.",
            leading = { IconBadge(icon = Icons.Rounded.Lock, tint = AppTheme.colors.accent) }
        )
    }
}

private const val MAX_DAYS = 90
