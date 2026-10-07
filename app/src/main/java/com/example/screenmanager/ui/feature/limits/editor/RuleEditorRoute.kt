package com.example.screenmanager.ui.feature.limits.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.screenmanager.model.AppLimitRule
import com.example.screenmanager.model.ScheduleRule
import com.example.screenmanager.model.SessionLimitRule
import com.example.screenmanager.ui.components.AppScreen
import com.example.screenmanager.ui.feature.limits.LimitsViewModel
import com.example.screenmanager.ui.feature.limits.RuleTarget
import com.example.screenmanager.ui.feature.limits.ShortsApps
import com.example.screenmanager.ui.theme.AppTheme
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

/**
 * Otvara formu za pravilo koje opisuje [target]: nalazi postojeće pravilo
 * (ili pravi podrazumevano novo), prikazuje odgovarajuću formu i prosleđuje
 * Save/Delete u [LimitsViewModel]. Posle Save/Delete zove [onDone].
 *
 * Forma drži radnu kopiju pravila (draft); u bazu se upisuje tek na Save.
 */
@Composable
fun RuleEditorRoute(
    target: RuleTarget,
    onDone: () -> Unit,
    viewModel: LimitsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val loaded = state
    if (loaded == null) {
        // Pravila se još učitavaju; forma mora da krene od stvarnih vrednosti.
        AppScreen(title = "", onBack = onDone) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppTheme.colors.accent)
            }
        }
        return
    }
    val labelFor = viewModel::appLabel

    when (target) {
        is RuleTarget.AppLimit -> {
            val existing = remember(target) { loaded.appLimits.firstOrNull { it.id == target.ruleId } }
            val initial = remember(target) { existing ?: newAppLimit(target.presetPackage, labelFor) }
            AppLimitEditor(
                initial = initial,
                isNew = existing == null,
                installedApps = loaded.installedApps,
                labelFor = labelFor,
                onSave = { viewModel.saveAppLimit(it); onDone() },
                onDelete = existing?.let { rule -> { viewModel.deleteAppLimit(rule.id); onDone() } },
                onBack = onDone
            )
        }

        is RuleTarget.SessionLimit -> {
            val existing = remember(target) { loaded.sessionLimits.firstOrNull { it.id == target.ruleId } }
            val initial = remember(target) { existing ?: newSessionLimit(target.presetPackage, labelFor) }
            SessionLimitEditor(
                initial = initial,
                isNew = existing == null,
                installedApps = loaded.installedApps,
                labelFor = labelFor,
                onSave = { viewModel.saveSessionLimit(it); onDone() },
                onDelete = existing?.let { rule -> { viewModel.deleteSessionLimit(rule.id); onDone() } },
                onBack = onDone
            )
        }

        is RuleTarget.Schedule -> {
            val existing = remember(target) { loaded.schedules.firstOrNull { it.id == target.ruleId } }
            val initial = remember(target) { existing ?: newSchedule(target.presetPackage) }
            ScheduleEditor(
                initial = initial,
                isNew = existing == null,
                installedApps = loaded.installedApps,
                labelFor = labelFor,
                onSave = { viewModel.saveSchedule(it); onDone() },
                onDelete = existing?.let { rule -> { viewModel.deleteSchedule(rule.id); onDone() } },
                onBack = onDone
            )
        }

        is RuleTarget.Shorts -> {
            val initial = remember(target) {
                val preset = target.presetPackage?.takeIf { it in ShortsApps.supported }
                if (preset == null) {
                    loaded.shorts
                } else {
                    // Dolazak sa ekrana aplikacije: ta aplikacija je unapred uključena.
                    loaded.shorts.copy(
                        selectedAppIds = (loaded.shorts.selectedAppIds + preset).distinct(),
                        isEnabled = true
                    )
                }
            }
            ShortsEditor(
                initial = initial,
                labelFor = labelFor,
                onSave = { viewModel.saveShorts(it); onDone() },
                onBack = onDone
            )
        }

        RuleTarget.MorningLock -> {
            val initial = remember(target) { loaded.morningLock }
            MorningLockEditor(
                initial = initial,
                installedApps = loaded.installedApps,
                labelFor = labelFor,
                onSave = { viewModel.saveMorningLock(it); onDone() },
                onBack = onDone
            )
        }
    }
}

// --- Podrazumevane vrednosti novih pravila ---

private fun newAppLimit(presetPackage: String?, labelFor: (String) -> String) = AppLimitRule(
    id = UUID.randomUUID().toString(),
    name = presetPackage?.let { "${labelFor(it)} limit" } ?: "Daily limit",
    selectedAppIds = listOfNotNull(presetPackage),
    dailyLimitMinutes = 60,
    blockDurationMinutes = 0
)

private fun newSessionLimit(presetPackage: String?, labelFor: (String) -> String) = SessionLimitRule(
    id = UUID.randomUUID().toString(),
    name = presetPackage?.let { "${labelFor(it)} sessions" } ?: "Session limit",
    selectedAppIds = listOfNotNull(presetPackage),
    sessionLengthMinutes = 5,
    maxSessions = 5,
    cooldownMinutes = 15
)

private fun newSchedule(presetPackage: String?) = ScheduleRule(
    id = UUID.randomUUID().toString(),
    name = "Focus time",
    startTime = LocalTime.of(9, 0),
    endTime = LocalTime.of(17, 0),
    daysOfWeek = setOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    ),
    selectedAppIds = listOfNotNull(presetPackage)
)
