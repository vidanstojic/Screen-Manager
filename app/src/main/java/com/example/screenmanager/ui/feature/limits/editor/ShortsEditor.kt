package com.example.screenmanager.ui.feature.limits.editor

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.screenmanager.model.ShortVideoConfig
import com.example.screenmanager.model.ShortsMode
import com.example.screenmanager.ui.common.formatMinutes
import com.example.screenmanager.ui.components.AppIcon
import com.example.screenmanager.ui.components.CardDivider
import com.example.screenmanager.ui.components.SegmentedControl
import com.example.screenmanager.ui.components.StepperRow
import com.example.screenmanager.ui.components.SwitchRow
import com.example.screenmanager.ui.feature.limits.ShortsApps
import com.example.screenmanager.ui.feature.limits.label
import com.example.screenmanager.ui.theme.Spacing

/**
 * Forma za globalno Shorts/Reels pravilo: gde važi (YouTube, Instagram) i
 * u kom modu (potpuna blokada / dnevni budžet / sesije).
 */
@Composable
fun ShortsEditor(
    initial: ShortVideoConfig,
    labelFor: (String) -> String,
    onSave: (ShortVideoConfig) -> Unit,
    onBack: () -> Unit
) {
    var draft by rememberSaveable { mutableStateOf(initial) }

    RuleEditorScaffold(
        title = "Shorts & Reels",
        onBack = onBack,
        canSave = true,
        onSave = { onSave(draft) }
    ) {
        FormCard(hint = "Only short videos are limited. Regular videos, the feed and messages keep working.") {
            SwitchRow(
                title = "Limit Shorts & Reels",
                checked = draft.isEnabled,
                onCheckedChange = { draft = draft.copy(isEnabled = it) }
            )
        }

        FormCard(title = "Apps") {
            ShortsApps.supported.forEachIndexed { index, packageName ->
                if (index > 0) CardDivider()
                val label = labelFor(packageName)
                SwitchRow(
                    title = label,
                    checked = packageName in draft.selectedAppIds,
                    onCheckedChange = { selected ->
                        val ids = if (selected) draft.selectedAppIds + packageName else draft.selectedAppIds - packageName
                        draft = draft.copy(selectedAppIds = ids.distinct())
                    },
                    leading = { AppIcon(packageName = packageName, label = label, size = 36.dp) }
                )
            }
        }

        FormCard(title = "Mode", hint = modeHint(draft.mode)) {
            SegmentedControl(
                options = ShortsMode.entries,
                selected = draft.mode,
                onSelected = { draft = draft.copy(mode = it) },
                label = { it.label },
                modifier = Modifier.padding(Spacing.sm)
            )
            when (draft.mode) {
                ShortsMode.BLOCKED -> Unit

                ShortsMode.BUDGET -> {
                    CardDivider()
                    StepperRow(
                        label = "Time per day",
                        value = draft.maxReelsWatchMinutes,
                        onValueChange = { draft = draft.copy(maxReelsWatchMinutes = it) },
                        range = 0..120,
                        step = 5,
                        valueLabel = ::formatMinutes
                    )
                    CardDivider()
                    StepperRow(
                        label = "Then block the whole app for",
                        value = draft.fullAppBlockMinutes,
                        onValueChange = { draft = draft.copy(fullAppBlockMinutes = it) },
                        range = 5..240,
                        step = 5,
                        valueLabel = ::formatMinutes
                    )
                }

                ShortsMode.SESSIONS -> {
                    CardDivider()
                    SessionSteppers(
                        sessionLengthMinutes = draft.sessionLengthMinutes,
                        maxSessions = draft.maxSessions,
                        cooldownMinutes = draft.cooldownMinutes,
                        onSessionLengthChange = { draft = draft.copy(sessionLengthMinutes = it) },
                        onMaxSessionsChange = { draft = draft.copy(maxSessions = it) },
                        onCooldownChange = { draft = draft.copy(cooldownMinutes = it) }
                    )
                }
            }
        }
    }
}

private fun modeHint(mode: ShortsMode): String = when (mode) {
    ShortsMode.BLOCKED ->
        "Every time Shorts or Reels open, you are sent straight back."

    ShortsMode.BUDGET ->
        "The budget is counted per app. When it runs out, the whole app is blocked for the set " +
            "time and Shorts/Reels stay closed until midnight."

    ShortsMode.SESSIONS ->
        "Only time inside Shorts/Reels counts. They are closed during the break and, after the " +
            "last session, until midnight."
}
