package com.example.screenmanager.ui.common

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView

/** Koliko se element smanji dok je pritisnut. */
private const val PRESSED_SCALE = 0.97f

/**
 * Element se blago "utisne" dok ga prst drži. [interactionSource] mora biti
 * isti onaj koji je prosleđen `clickable`-u, da bi se znalo kad je pritisnut.
 */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) PRESSED_SCALE else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 900f),
        label = "pressScale"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Kratak, jedva primetan "klik" vibracijom — za izbore (switch, segment, dan).
 * Poštuje sistemsko podešavanje vibracije na dodir.
 */
@Composable
fun rememberTickHaptic(): () -> Unit {
    val view = LocalView.current
    return remember(view) { { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) } }
}
