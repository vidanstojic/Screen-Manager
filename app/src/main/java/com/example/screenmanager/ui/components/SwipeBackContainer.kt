package com.example.screenmanager.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * "iOS-style" swipe-back gest: prevlačenje prsta sa LEVE ivice ekrana udesno
 * vraća korisnika na prethodno posećenu stranicu (poziva [onBack]).
 *
 * Nema dugme - samo gest, sa vizuelnim praćenjem prsta (sadržaj se pomera
 * zajedno sa prstom, a ako se ne prevuče dovoljno, elastično se vrati nazad).
 */
@Composable
fun SwipeBackContainer(
    onBack: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val edgeWidthPx = with(density) { 24.dp.toPx() }
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val dragThresholdPx = screenWidthPx * 0.25f

    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var startedAtEdge by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        startedAtEdge = offset.x <= edgeWidthPx
                    },
                    onDragEnd = {
                        if (startedAtEdge) {
                            scope.launch {
                                if (offsetX.value > dragThresholdPx) {
                                    offsetX.animateTo(screenWidthPx, tween(220))
                                    onBack()
                                    offsetX.snapTo(0f)
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