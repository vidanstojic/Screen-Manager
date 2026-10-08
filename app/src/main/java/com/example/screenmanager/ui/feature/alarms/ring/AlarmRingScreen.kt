package com.example.screenmanager.ui.feature.alarms.ring

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.screenmanager.alarm.domain.RingReason
import com.example.screenmanager.alarm.domain.effectiveMissionType
import com.example.screenmanager.alarm.service.StepSensor
import com.example.screenmanager.model.AlarmMission
import com.example.screenmanager.model.MissionType
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.example.screenmanager.alarm.service.ActiveRing
import com.example.screenmanager.ui.common.formatClock
import com.example.screenmanager.ui.common.pluralize
import com.example.screenmanager.ui.components.IconBadge
import com.example.screenmanager.ui.components.PrimaryButton
import com.example.screenmanager.ui.components.SecondaryButton
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.AuroraBackground
import com.example.screenmanager.ui.theme.Spacing
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Ulaz u ekran alarma. [onMissionInteraction] se zove na svaki dodir u
 * misiji (servis tada utiša zvuk).
 */
@Composable
fun AlarmRingRoute(
    ring: ActiveRing,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    onMissionInteraction: () -> Unit
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val context = LocalContext.current
    val missionType = remember(ring.startedAt) {
        effectiveMissionType(ring.alarm.mission, stepsAvailable = StepSensor(context).isAvailable)
    }
    var missionStarted by rememberSaveable(ring.startedAt) { mutableStateOf(false) }
    AlarmRingScreen(
        ring = ring,
        now = now,
        missionType = missionType,
        missionStarted = missionStarted,
        onStartMission = {
            missionStarted = true
            onMissionInteraction()
        },
        onSnooze = onSnooze,
        onDismiss = onDismiss,
        onMissionInteraction = onMissionInteraction
    )
}

/**
 * Sat, naziv alarma i dugmad. Bez misije: Snooze / Dismiss. Sa misijom:
 * Snooze / "Start mission"; posle starta misija zauzima sredinu ekrana, a
 * alarm se gasi tek kad je rešena.
 */
@Composable
fun AlarmRingScreen(
    ring: ActiveRing,
    now: Long,
    missionType: MissionType,
    missionStarted: Boolean,
    onStartMission: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    onMissionInteraction: () -> Unit
) {
    val hasMission = missionType != MissionType.NONE
    val colors = AppTheme.colors
    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = Spacing.xxl, vertical = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(Spacing.xl))
            Text(
                text = DATE_FORMAT.format(Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textSecondary
            )
            Text(
                text = formatClock(now),
                style = MaterialTheme.typography.displayLarge.copy(
                    brush = Brush.linearGradient(listOf(Color.White, colors.accent))
                )
            )
            Text(
                text = ringTitle(ring),
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )

            if (hasMission && missionStarted) {
                Spacer(Modifier.height(Spacing.xl))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (missionType != ring.alarm.mission.type) {
                        Text(
                            text = "Step counting isn't available, so here's a math mission instead.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted,
                            modifier = Modifier.padding(bottom = Spacing.sm)
                        )
                    }
                    MissionPanel(
                        mission = ring.alarm.mission,
                        type = missionType,
                        onSolved = onDismiss,
                        onInteraction = onMissionInteraction
                    )
                }
                Spacer(Modifier.height(Spacing.lg))
            } else {
                Spacer(Modifier.weight(1f))
                Box(contentAlignment = Alignment.Center) {
                    PulseRings(size = 240.dp)
                    IconBadge(icon = Icons.Rounded.Alarm, size = 88.dp)
                }
                Spacer(Modifier.weight(1f))
            }

            if (ring.reason == RingReason.TEST) {
                Text(
                    text = "Test ring: no snooze, detox or wake-up check.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(Spacing.md))
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                if (ring.canSnooze) {
                    SecondaryButton(
                        text = "Snooze ${ring.alarm.snooze.minutes} min · ${pluralize(ring.snoozesLeft, "left", "left")}",
                        icon = Icons.Rounded.Snooze,
                        onClick = onSnooze,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                when {
                    !hasMission -> PrimaryButton(text = "Dismiss", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                    !missionStarted -> PrimaryButton(
                        text = "Start mission · ${missionLabel(ring.alarm.mission, missionType)}",
                        onClick = onStartMission,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private fun missionLabel(mission: AlarmMission, type: MissionType): String = when (type) {
    MissionType.MATH -> pluralize(mission.count, "problem")
    MissionType.TYPING -> if (mission.customText.isNotBlank()) "type your text" else pluralize(mission.count, "sentence")
    MissionType.STEPS -> "${mission.steps} steps"
    MissionType.NONE -> ""
}

private fun ringTitle(ring: ActiveRing): String {
    val label = ring.alarm.label.ifBlank { "Alarm" }
    return when (ring.reason) {
        RingReason.WAKE_CHECK -> "$label · you didn't confirm you're awake"
        RingReason.SNOOZE -> "$label · snoozed ${pluralize(ring.snoozeCount, "time")}"
        else -> label
    }
}

/** Krugovi koji se šire iz centra i nestaju — ritam zvonjenja. */
@Composable
private fun PulseRings(size: Dp) {
    val transition = rememberInfiniteTransition(label = "ringPulse")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_400, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )
    val color = AppTheme.colors.accentStart
    Canvas(modifier = Modifier.size(size)) {
        val maxRadius = this.size.minDimension / 2f
        repeat(3) { index ->
            val t = (phase + index / 3f) % 1f
            drawCircle(
                color = color.copy(alpha = (1f - t) * 0.55f),
                radius = maxRadius * (0.35f + 0.65f * t),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)
