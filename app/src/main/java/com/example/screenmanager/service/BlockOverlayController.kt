package com.example.screenmanager.service

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.annotation.MainThread
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.screenmanager.domain.rules.BlockDecision
import com.example.screenmanager.ui.feature.blocking.BlockScreen
import com.example.screenmanager.ui.theme.ScreenManagerTheme

/**
 * Prikazuje full-screen blok preko zabranjene aplikacije. Ovde je samo
 * upravljanje prozorom; izgled je u `ui/feature/blocking/BlockScreen.kt`.
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
                    decisionState.value?.let { BlockScreen(decision = it, onGoHome = onGoHome) }
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
