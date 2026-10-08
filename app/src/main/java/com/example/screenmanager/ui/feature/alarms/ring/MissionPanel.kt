package com.example.screenmanager.ui.feature.alarms.ring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.screenmanager.alarm.domain.MathMissions
import com.example.screenmanager.alarm.domain.StepMissions
import com.example.screenmanager.alarm.domain.TypingMissions
import com.example.screenmanager.alarm.service.StepSensor
import com.example.screenmanager.model.AlarmMission
import com.example.screenmanager.model.MissionType
import com.example.screenmanager.ui.components.AppCard
import com.example.screenmanager.ui.components.PrimaryButton
import com.example.screenmanager.ui.components.ProgressRing
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing
import kotlin.random.Random

/**
 * Misija na ekranu alarma. [onSolved] se zove jednom, kad je ceo zadatak
 * rešen; [onInteraction] na svaki unos (servis tada utiša zvuk).
 *
 * Zadaci se prave iz semena sačuvanog u `rememberSaveable`, pa rotacija
 * ekrana ne menja račun koji korisnik upravo rešava.
 */
@Composable
fun MissionPanel(
    mission: AlarmMission,
    type: MissionType,
    onSolved: () -> Unit,
    onInteraction: () -> Unit
) {
    val seed = rememberSaveable { Random.nextLong() }
    when (type) {
        MissionType.MATH -> MathMission(mission, seed, onSolved, onInteraction)
        MissionType.TYPING -> TypingMission(mission, seed, onSolved, onInteraction)
        MissionType.STEPS -> StepsMission(mission.steps, onSolved, onInteraction)
        MissionType.NONE -> LaunchedEffect(Unit) { onSolved() }
    }
}

@Composable
private fun MathMission(mission: AlarmMission, seed: Long, onSolved: () -> Unit, onInteraction: () -> Unit) {
    val problems = remember(seed) { MathMissions.generate(mission.difficulty, mission.count, Random(seed)) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var input by rememberSaveable { mutableStateOf("") }
    var wrong by rememberSaveable { mutableStateOf(false) }
    val problem = problems[index.coerceAtMost(problems.lastIndex)]

    val check: () -> Unit = {
        onInteraction()
        if (MathMissions.isCorrect(problem, input)) {
            wrong = false
            input = ""
            if (index + 1 >= problems.size) onSolved() else index++
        } else {
            wrong = true
            input = ""
        }
    }

    MissionCard(title = "Solve ${index + 1} of ${problems.size}") {
        Text(
            text = "${problem.text} = ?",
            style = MaterialTheme.typography.displaySmall,
            color = AppTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        MissionTextField(
            value = input,
            onValueChange = {
                input = it.filter(Char::isDigit).take(6)
                wrong = false
                onInteraction()
            },
            placeholder = "Answer",
            keyboardType = KeyboardType.NumberPassword,
            onDone = check
        )
        if (wrong) ErrorLine("Not quite — try again.")
        PrimaryButton(text = "Check", onClick = check, enabled = input.isNotEmpty(), modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TypingMission(mission: AlarmMission, seed: Long, onSolved: () -> Unit, onInteraction: () -> Unit) {
    val phrases = remember(seed) { TypingMissions.pick(mission, Random(seed)) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var input by rememberSaveable { mutableStateOf("") }
    val phrase = phrases[index.coerceAtMost(phrases.lastIndex)]
    val correct = TypingMissions.correctPrefixLength(phrase, input)

    MissionCard(title = "Type ${index + 1} of ${phrases.size}") {
        val colors = AppTheme.colors
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = colors.accent)) { append(phrase.take(correct)) }
                withStyle(SpanStyle(color = colors.textPrimary)) { append(phrase.drop(correct)) }
            },
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        MissionTextField(
            value = input,
            onValueChange = { typed ->
                input = typed
                onInteraction()
                if (TypingMissions.matches(phrase, typed)) {
                    input = ""
                    if (index + 1 >= phrases.size) onSolved() else index++
                }
            },
            placeholder = "Type the sentence above",
            keyboardType = KeyboardType.Text,
            onDone = {}
        )
        Text(
            text = "Capital letters and extra spaces don't matter.",
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.colors.textMuted
        )
    }
}

@Composable
private fun StepsMission(goal: Int, onSolved: () -> Unit, onInteraction: () -> Unit) {
    val context = LocalContext.current
    var steps by rememberSaveable { mutableIntStateOf(0) }
    val currentOnSolved by rememberUpdatedState(onSolved)
    val currentOnInteraction by rememberUpdatedState(onInteraction)

    DisposableEffect(Unit) {
        val sensor = StepSensor(context)
        sensor.start {
            if (steps < goal) {
                steps++
                currentOnInteraction()
                if (StepMissions.isDone(steps, goal)) currentOnSolved()
            }
        }
        onDispose { sensor.stop() }
    }

    MissionCard(title = "Walk $goal steps") {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            ProgressRing(progress = StepMissions.progress(steps, goal), size = 180.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$steps", style = MaterialTheme.typography.displaySmall, color = AppTheme.colors.textPrimary)
                    Text("of $goal", style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textMuted)
                }
            }
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = "Get out of bed and walk around — keep the phone in your hand.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MissionCard(title: String, content: @Composable () -> Unit) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(text = title.uppercase(), style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textMuted)
            content()
        }
    }
}

@Composable
private fun MissionTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    onDone: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        placeholder = { Text(placeholder) },
        singleLine = keyboardType != KeyboardType.Text,
        textStyle = MaterialTheme.typography.titleLarge,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Done,
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AppTheme.colors.textPrimary,
            unfocusedTextColor = AppTheme.colors.textPrimary,
            focusedBorderColor = AppTheme.colors.accent,
            unfocusedBorderColor = AppTheme.colors.glassBorderTop,
            cursorColor = AppTheme.colors.accent,
            focusedPlaceholderColor = AppTheme.colors.textMuted,
            unfocusedPlaceholderColor = AppTheme.colors.textMuted
        )
    )
}

@Composable
private fun ErrorLine(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.warning)
}
