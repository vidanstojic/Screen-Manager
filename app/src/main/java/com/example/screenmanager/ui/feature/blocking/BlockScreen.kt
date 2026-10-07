package com.example.screenmanager.ui.feature.blocking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.screenmanager.domain.rules.BlockDecision
import com.example.screenmanager.ui.common.formatClock
import com.example.screenmanager.ui.common.formatCountdown
import com.example.screenmanager.ui.components.IconBadge
import com.example.screenmanager.ui.components.PrimaryButton
import com.example.screenmanager.ui.theme.AppTheme
import com.example.screenmanager.ui.theme.Spacing
import kotlinx.coroutines.delay

/**
 * Blok ekran koji se crta PREKO zabranjene aplikacije (prozor pravi
 * `service/BlockOverlayController`): razlog blokade, odbrojavanje do kraja
 * i dugme za povratak na početni ekran telefona.
 */
@Composable
fun BlockScreen(
    decision: BlockDecision,
    onGoHome: () -> Unit
) {
    var remainingMs by remember(decision.blockedUntil) { mutableLongStateOf(decision.remainingMs()) }

    LaunchedEffect(decision.blockedUntil) {
        while (remainingMs > 0) {
            delay(1_000)
            remainingMs = decision.remainingMs()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .systemBarsPadding()
            .padding(horizontal = Spacing.xxl, vertical = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(1f))

        IconBadge(icon = Icons.Rounded.Lock, size = 72.dp)
        Spacer(Modifier.height(Spacing.xl))
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
        Text(
            text = formatCountdown(remainingMs),
            style = MaterialTheme.typography.displayLarge,
            color = AppTheme.colors.accent
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "Available again at ${formatClock(decision.blockedUntil)}",
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.textMuted
        )

        Spacer(Modifier.weight(1f))

        PrimaryButton(
            text = "Go to home screen",
            onClick = onGoHome,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
