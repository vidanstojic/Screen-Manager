package com.example.screenmanager.ui.feature.limits

import android.os.SystemClock
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.screenmanager.domain.rules.LimitsLock
import com.example.screenmanager.ui.common.formatClock
import com.example.screenmanager.ui.common.relativeDayLabel
import com.example.screenmanager.ui.common.rememberNow
import com.example.screenmanager.ui.components.AppCard
import com.example.screenmanager.ui.components.CardDivider
import com.example.screenmanager.ui.components.IconBadge
import com.example.screenmanager.ui.components.ListRow
import com.example.screenmanager.ui.components.SecondaryButton
import com.example.screenmanager.ui.components.SegmentedControl
import com.example.screenmanager.ui.components.TextAction
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Kraj zaključavanja za formu pravila koje je već sačuvano; `null` = forma je slobodna
 * (nema zaključavanja ili je pravilo novo). Postavlja ga RuleEditorRoute, čita RuleEditorScaffold.
 */
val LocalRuleLockedUntil = compositionLocalOf<Long?> { null }

/** Koliko dugo se zaključava (izbor na Limits ekranu). */
enum class LockDuration(val label: String, val durationMs: Long) {
    HOUR("1 hour", LimitsLock.HOUR_MS),
    DAY("1 day", LimitsLock.DAY_MS),
    WEEK("1 week", 7 * LimitsLock.DAY_MS)
}

/** Kraj aktivnog zaključavanja (osvežava se dok je ekran otvoren), ili `null`. */
@Composable
fun rememberLockedUntil(lock: LimitsLock?): Long? {
    val now by rememberNow(intervalMs = 15_000L)
    val elapsed = SystemClock.elapsedRealtime()
    return lock?.takeIf { it.isActive(now, elapsed) }?.effectiveEnd(now, elapsed)
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

/**
 * Zaključavanje limita na Limits ekranu. Pre zaključavanja potvrda, jer ne može da se
 * poništi; dok traje, kartica samo kaže do kada.
 */
@Composable
fun LockLimitsCard(lockedUntil: Long?, onLock: (Long) -> Unit) {
    var duration by rememberSaveable { mutableStateOf(LockDuration.DAY) }
    var confirm by rememberSaveable { mutableStateOf(false) }

    if (confirm) {
        val endsAt = System.currentTimeMillis() + duration.durationMs
        AlertDialog(
            onDismissRequest = { confirm = false },
            containerColor = AppTheme.colors.surface,
            title = { Text("Lock limits for ${duration.label}?", color = AppTheme.colors.textPrimary) },
            text = {
                Text(
                    "Until ${formatLockEnd(endsAt)} you won't be able to turn off, edit or delete your limits, " +
                        "and the emergency pause won't be available. You can still add new limits or turn limits on. " +
                        "The lock can't be undone.",
                    color = AppTheme.colors.textSecondary
                )
            },
            confirmButton = {
                TextAction(text = "Lock", onClick = {
                    confirm = false
                    onLock(duration.durationMs)
                })
            },
            dismissButton = { TextAction(text = "Cancel", color = AppTheme.colors.textSecondary, onClick = { confirm = false }) }
        )
    }

    AppCard(contentPadding = PaddingValues(0.dp)) {
        ListRow(
            title = if (lockedUntil != null) "Limits are locked" else "Lock limits",
            subtitle = if (lockedUntil != null) {
                "Until ${formatLockEnd(lockedUntil)}. Limits can't be turned off, edited or deleted until then; " +
                    "you can still add new ones."
            } else {
                "Keep yourself from turning limits off on impulse: while locked, limits can't be turned off, " +
                    "edited or deleted."
            },
            leading = {
                IconBadge(
                    icon = if (lockedUntil != null) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                    tint = if (lockedUntil != null) AppTheme.colors.accent else AppTheme.colors.textSecondary
                )
            }
        )
        if (lockedUntil == null) {
            CardDivider()
            SegmentedControl(
                options = LockDuration.entries,
                selected = duration,
                onSelected = { duration = it },
                label = LockDuration::label,
                modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md)
            )
            SecondaryButton(
                text = "Lock for ${duration.label}",
                icon = Icons.Rounded.Lock,
                onClick = { confirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg)
            )
        }
    }
}

/** Na vrhu forme sačuvanog pravila dok su limiti zaključani (Save i brisanje su tada isključeni). */
@Composable
fun LockedRuleBanner(lockedUntil: Long) {
    AppCard(contentPadding = PaddingValues(0.dp), borderColor = AppTheme.colors.accent) {
        ListRow(
            title = "Limits are locked",
            subtitle = "Until ${formatLockEnd(lockedUntil)}. This rule can't be changed or deleted until then.",
            leading = { IconBadge(icon = Icons.Rounded.Lock, tint = AppTheme.colors.accent) }
        )
    }
}
