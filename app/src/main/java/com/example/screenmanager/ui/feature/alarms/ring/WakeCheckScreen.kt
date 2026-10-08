package com.example.screenmanager.ui.feature.alarms.ring

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WbSunny
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.screenmanager.alarm.domain.WakeCheckPolicy
import com.example.screenmanager.ui.common.formatCountdown
import com.example.screenmanager.ui.components.IconBadge
import com.example.screenmanager.ui.components.PrimaryButton
import com.example.screenmanager.ui.components.ProgressRing
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.AuroraBackground
import com.example.screenmanager.ui.theme.Spacing
import kotlinx.coroutines.delay

/** "Are you awake?" sa odbrojavanjem do ponovnog zvonjenja i jednim velikim dugmetom. */
@Composable
fun WakeCheckScreen(deadline: Long, onConfirm: () -> Unit) {
    var remaining by remember { mutableLongStateOf((deadline - System.currentTimeMillis()).coerceAtLeast(0)) }
    LaunchedEffect(deadline) {
        while (remaining > 0) {
            delay(1_000)
            remaining = (deadline - System.currentTimeMillis()).coerceAtLeast(0)
        }
    }
    val progress by animateFloatAsState(
        targetValue = remaining.toFloat() / WakeCheckPolicy.RESPONSE_WINDOW_MS,
        animationSpec = tween(1_000, easing = LinearEasing),
        label = "wakeCheck"
    )

    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = Spacing.xxl, vertical = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.weight(1f))
            IconBadge(icon = Icons.Rounded.WbSunny, size = 64.dp)
            Spacer(Modifier.height(Spacing.lg))
            Text("Are you awake?", style = MaterialTheme.typography.headlineMedium, color = AppTheme.colors.textPrimary)
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "If you don't confirm in time, the alarm rings again.",
                style = MaterialTheme.typography.bodyLarge,
                color = AppTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Spacing.xxl))
            ProgressRing(progress = progress, size = 200.dp) {
                Text(formatCountdown(remaining), style = MaterialTheme.typography.displaySmall, color = AppTheme.colors.textPrimary)
            }
            Spacer(Modifier.weight(1f))
            PrimaryButton(text = "I'm awake", onClick = onConfirm, modifier = Modifier.fillMaxWidth())
        }
    }
}
