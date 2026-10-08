package com.example.screenmanager.ui.feature.blocking

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.screenmanager.domain.rules.BlockDecision
import com.example.screenmanager.ui.common.formatClock
import com.example.screenmanager.ui.common.formatCountdown
import com.example.screenmanager.ui.components.IconBadge
import com.example.screenmanager.ui.components.PrimaryButton
import com.example.screenmanager.ui.components.ProgressRing
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.AuroraBackground
import com.example.screenmanager.ui.theme.Spacing
import kotlinx.coroutines.delay

/**
 * Blok ekran koji se crta PREKO zabranjene aplikacije (prozor pravi
 * `service/BlockOverlayController`): razlog blokade, odbrojavanje do kraja
 * i dugme za povratak na početni ekran telefona. Zaseban je prozor, pa
 * crta svoju aurora pozadinu (neprovidnu — aplikacija ispod se ne vidi).
 *
 * Odbrojavanje je prsten koji se prazni (pun kad se ekran pojavi), a iza
 * njega "diše" mek sjaj — ritam za par mirnih udisaja umesto skrolovanja.
 */
@Composable
fun BlockScreen(
    decision: BlockDecision,
    onGoHome: () -> Unit
) {
    var remainingMs by remember(decision.blockedUntil) { mutableLongStateOf(decision.remainingMs()) }
    // Prsten meri preostalo vreme u odnosu na ono kad se ekran pojavio.
    val initialMs = remember(decision.blockedUntil) { decision.remainingMs().coerceAtLeast(1L) }
    val ringProgress by animateFloatAsState(
        targetValue = remainingMs.toFloat() / initialMs,
        animationSpec = tween(durationMillis = 1_000, easing = LinearEasing),
        label = "blockRing"
    )

    LaunchedEffect(decision.blockedUntil) {
        while (remainingMs > 0) {
            delay(1_000)
            remainingMs = decision.remainingMs()
        }
    }

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

            IconBadge(icon = Icons.Rounded.Lock, size = 56.dp)
            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = "This app is blocked",
                style = MaterialTheme.typography.headlineMedium,
                color = AppTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = decision.message,
                style = MaterialTheme.typography.bodyLarge,
                color = AppTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Spacing.xxl))
            Box(contentAlignment = Alignment.Center) {
                BreathingGlow(size = RingSize + 56.dp)
                ProgressRing(progress = ringProgress, size = RingSize, strokeWidth = 14.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatCountdown(remainingMs),
                            style = MaterialTheme.typography.displaySmall,
                            color = AppTheme.colors.textPrimary,
                            maxLines = 1
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "until ${formatClock(decision.blockedUntil)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTheme.colors.textMuted
                        )
                    }
                }
            }
            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = "Breathe in as the light grows, out as it fades.",
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.textMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(1f))

            PrimaryButton(
                text = "Go to home screen",
                onClick = onGoHome,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private val RingSize = 220.dp

/** Mek radijalni sjaj koji se polako širi i skuplja (~4 s udah, ~4 s izdah). */
@Composable
private fun BreathingGlow(size: Dp) {
    val transition = rememberInfiniteTransition(label = "breathing")
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )
    val glow = AppTheme.colors.accentStart
    Canvas(modifier = Modifier.size(size)) {
        val radius = this.size.minDimension / 2f * (0.72f + 0.28f * breath)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glow.copy(alpha = 0.10f + 0.25f * breath), glow.copy(alpha = 0f)),
                center = center,
                radius = radius
            ),
            radius = radius
        )
    }
}
