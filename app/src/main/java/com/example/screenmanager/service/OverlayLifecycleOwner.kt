package com.example.screenmanager.service

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/**
 * Lifecycle + SavedState vlasnik za ComposeView koji živi u WindowManager-u
 * (van Activity-ja).
 *
 * Compose zahteva i ViewTreeSavedStateRegistryOwner — bez njega
 * `addView` pada sa "Composed into the View which doesn't propagate
 * ViewTreeSavedStateRegistryOwner". Sve metode moraju da se zovu na main thread-u
 * (LifecycleRegistry to proverava).
 */
class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = registry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    fun start() {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun stop() {
        if (registry.currentState == Lifecycle.State.INITIALIZED) return
        registry.currentState = Lifecycle.State.DESTROYED
    }
}
