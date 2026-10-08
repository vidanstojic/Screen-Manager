package com.example.screenmanager.ui.feature.alarms

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.screenmanager.alarm.domain.MathMissions
import com.example.screenmanager.alarm.domain.StepMissions
import com.example.screenmanager.alarm.domain.TypingMissions
import com.example.screenmanager.alarm.service.StepSensor
import com.example.screenmanager.model.AlarmMission
import com.example.screenmanager.model.MissionDifficulty
import com.example.screenmanager.model.MissionType
import com.example.screenmanager.ui.common.OnResume
import com.example.screenmanager.ui.components.AppCard
import com.example.screenmanager.ui.components.AppTextField
import com.example.screenmanager.ui.components.CardDivider
import com.example.screenmanager.ui.components.SegmentedControl
import com.example.screenmanager.ui.components.StepperRow
import com.example.screenmanager.ui.components.TextAction
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing
import kotlin.random.Random

/**
 * Misija za gašenje alarma: bez misije, računi, prekucavanje ili koraci.
 * Sa misijom alarm može samo da se odloži dok zadatak nije rešen.
 */
@Composable
fun AlarmMissionSection(mission: AlarmMission, onChange: (AlarmMission) -> Unit) {
    EditorSection(title = "Wake-up mission") {
        SegmentedControl(
            options = MissionType.entries,
            selected = mission.type,
            onSelected = { onChange(mission.copy(type = it)) },
            label = ::missionTypeLabel
        )
        if (mission.type != MissionType.NONE) {
            AppCard(contentPadding = PaddingValues(vertical = Spacing.xs)) {
                when (mission.type) {
                    MissionType.MATH -> MathOptions(mission, onChange)
                    MissionType.TYPING -> TypingOptions(mission, onChange)
                    MissionType.STEPS -> StepsOptions(mission, onChange)
                    MissionType.NONE -> Unit
                }
            }
        }
        EditorHint(
            when (mission.type) {
                MissionType.NONE -> "The alarm can be dismissed with one tap."
                else -> "The alarm turns off only after the mission is done. While you solve it, the sound gets quieter."
            }
        )
    }
}

fun missionTypeLabel(type: MissionType): String = when (type) {
    MissionType.NONE -> "None"
    MissionType.MATH -> "Math"
    MissionType.TYPING -> "Typing"
    MissionType.STEPS -> "Steps"
}

@Composable
private fun MathOptions(mission: AlarmMission, onChange: (AlarmMission) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
        Text("Difficulty", style = MaterialTheme.typography.titleSmall, color = AppTheme.colors.textPrimary)
        Text(
            text = "e.g. " + remember(mission.difficulty) { MathMissions.generate(mission.difficulty, Random(42)).text },
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = Spacing.sm)
        )
        SegmentedControl(
            options = MissionDifficulty.entries,
            selected = mission.difficulty,
            onSelected = { onChange(mission.copy(difficulty = it)) },
            label = { it.name.lowercase().replaceFirstChar(Char::uppercase) }
        )
    }
    CardDivider()
    StepperRow(
        label = "Problems",
        value = mission.count.coerceIn(1, MathMissions.MAX_COUNT),
        onValueChange = { onChange(mission.copy(count = it)) },
        range = 1..MathMissions.MAX_COUNT
    )
}

@Composable
private fun TypingOptions(mission: AlarmMission, onChange: (AlarmMission) -> Unit) {
    if (mission.customText.isBlank()) {
        StepperRow(
            label = "Sentences",
            value = mission.count.coerceIn(1, MAX_SENTENCES),
            onValueChange = { onChange(mission.copy(count = it)) },
            range = 1..MAX_SENTENCES,
            supportingText = "e.g. \"${TypingMissions.phrases.first()}\""
        )
        CardDivider()
    }
    Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
        AppTextField(
            value = mission.customText,
            onValueChange = { onChange(mission.copy(customText = it.take(MAX_CUSTOM_TEXT))) },
            label = "Your own sentence (optional)",
            placeholder = "e.g. I get up the moment my alarm rings"
        )
    }
}

@Composable
private fun StepsOptions(mission: AlarmMission, onChange: (AlarmMission) -> Unit) {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(StepSensor.hasPermission(context)) }
    val hasSensor = remember { StepSensor.hasSensor(context) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
    }
    OnResume { permissionGranted = StepSensor.hasPermission(context) }

    StepperRow(
        label = "Steps",
        value = mission.steps.coerceIn(StepMissions.MIN_STEPS, StepMissions.MAX_STEPS),
        onValueChange = { onChange(mission.copy(steps = it)) },
        range = StepMissions.MIN_STEPS..StepMissions.MAX_STEPS,
        step = 10
    )
    when {
        !hasSensor -> StepsWarning("This phone has no step sensor — a math mission will be used instead.")
        !permissionGranted -> {
            CardDivider()
            Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
                Text(
                    text = "Step counting needs the Physical activity permission. Without it a math mission is used.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.warning
                )
                TextAction(
                    text = "Allow physical activity",
                    onClick = { permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) }
                )
            }
        }
    }
}

@Composable
private fun StepsWarning(text: String) {
    CardDivider()
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = AppTheme.colors.warning,
        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)
    )
}

private const val MAX_SENTENCES = 5
private const val MAX_CUSTOM_TEXT = 200
