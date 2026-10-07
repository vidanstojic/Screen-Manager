package com.example.screenmanager.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * "iOS-style" swipe-back: prevlačenje sa LEVE ivice ekrana udesno vraća na
 * prethodni ekran ([onBack]). Sadržaj prati prst; ako se ne prevuče dovoljno,
 * elastično se vrati.
 *
 * Posle uspešnog gesta sadržaj OSTAJE van ekrana — [AppNavHost] ga zatim
 * uklanja, pa nema treptaja starog ekrana.
 */
@Composable
fun SwipeBackContainer(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val edgeWidthPx = with(density) { 24.dp.toPx() }
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val dragThresholdPx = screenWidthPx * 0.25f

    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val currentOnBack by rememberUpdatedState(onBack)
    var startedAtEdge by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> startedAtEdge = offset.x <= edgeWidthPx },
                    onDragEnd = {
                        if (startedAtEdge) {
                            scope.launch {
                                if (offsetX.value > dragThresholdPx) {
                                    offsetX.animateTo(screenWidthPx, tween(220))
                                    currentOnBack()
                                } else {
                                    offsetX.animateTo(0f, spring())
                                }
                            }
                        }
                        startedAtEdge = false
                    },
                    onDragCancel = {
                        scope.launch { offsetX.animateTo(0f, spring()) }
                        startedAtEdge = false
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        if (startedAtEdge) {
                            scope.launch {
                                offsetX.snapTo((offsetX.value + dragAmount).coerceIn(0f, screenWidthPx))
                            }
                            change.consume()
                        }
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = offsetX.value }
        ) {
            content()
        }
    }
}
