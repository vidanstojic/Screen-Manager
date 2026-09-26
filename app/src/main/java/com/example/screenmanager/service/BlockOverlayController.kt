package com.example.screenmanager.service

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.annotation.MainThread
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.screenmanager.domain.rules.BlockDecision
import com.example.screenmanager.ui.theme.ScreenManagerTheme
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Prikazuje full-screen blok preko zabranjene aplikacije.
 *
 * SVE metode su @MainThread — ranije su pozivane sa Dispatchers.Default,
 * što ruši WindowManager/LifecycleRegistry. Jedan ComposeView se
 * re-koristi; nova odluka samo menja state (bez remove/add treptanja).
 */
class BlockOverlayController(
    private val context: Context,
    private val onGoHome: () -> Unit
) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private val decisionState: MutableState<BlockDecision?> = mutableStateOf(null)

    val isShowing: Boolean
        get() = composeView != null

    @MainThread
    fun show(decision: BlockDecision) {
        if (!Settings.canDrawOverlays(context)) return
        decisionState.value = decision
        if (composeView != null) return

        val owner = OverlayLifecycleOwner().also { it.start() }
        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                ScreenManagerTheme {
                    decisionState.value?.let { BlockOverlay(decision = it, onGoHome = onGoHome) }
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        runCatching { windowManager.addView(view, params) }
            .onSuccess {
                composeView = view
                lifecycleOwner = owner
            }
            .onFailure { owner.stop() }
    }

    @MainThread
    fun hide() {
        val view = composeView ?: return
        runCatching { windowManager.removeView(view) }
        lifecycleOwner?.stop()
        composeView = null
        lifecycleOwner = null
        decisionState.value = null
    }
}

@Composable
private fun BlockOverlay(
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF216171A))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Aplikacija je blokirana",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = decision.message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFE5E7EB),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = formatRemaining(remainingMs),
                style = MaterialTheme.typography.displaySmall,
                color = Color(0xFF93C5FD)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onGoHome) {
                Text("Nazad na Home")
            }
        }
    }
}

/** HH:MM:SS za duže blokade (do ponoći), MM:SS za kratke. */
private fun formatRemaining(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
