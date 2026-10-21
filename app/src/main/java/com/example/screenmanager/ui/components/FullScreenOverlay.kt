package com.example.screenmanager.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.example.screenmanager.ui.theme.AuroraBackground

/** Jedan sloj preko celog ekrana koji crta [OverlayHost]. */
@Stable
class OverlayHostState {
    internal var content: (@Composable () -> Unit)? by mutableStateOf(null)
}

private val LocalOverlayHost = staticCompositionLocalOf<OverlayHostState?> { null }

/**
 * Koren UI-a: iznad [content] crta sloj koji je otvorio [FullScreenOverlay].
 *
 * Sloj je u prozoru aktivnosti, pa sistemske trake i tastatura rade kao na svakom
 * ekranu ([AppScreen]). `Dialog` je zaseban prozor i na nekim telefonima nije
 * dobijao visinu navigacione trake — dugme na dnu je padalo ispod nje.
 */
@Composable
fun OverlayHost(content: @Composable () -> Unit) {
    val state = remember { OverlayHostState() }
    CompositionLocalProvider(LocalOverlayHost provides state) {
        Box(Modifier.fillMaxSize()) {
            content()
            state.content?.let { overlay ->
                AuroraBackground(
                    modifier = Modifier
                        .fillMaxSize()
                        // Dodiri ne prolaze do ekrana ispod.
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                ) {
                    overlay()
                }
            }
        }
    }
}

/**
 * Prikazuje [content] preko celog ekrana dok je ovaj composable u kompoziciji;
 * sistemski "nazad" zove [onDismiss]. Mora biti unutar [OverlayHost].
 */
@Composable
fun FullScreenOverlay(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val host = checkNotNull(LocalOverlayHost.current) { "FullScreenOverlay needs an OverlayHost above it" }
    val currentContent by rememberUpdatedState(content)
    BackHandler(onBack = onDismiss)
    DisposableEffect(host) {
        val entry: @Composable () -> Unit = { currentContent() }
        host.content = entry
        onDispose { if (host.content === entry) host.content = null }
    }
}
