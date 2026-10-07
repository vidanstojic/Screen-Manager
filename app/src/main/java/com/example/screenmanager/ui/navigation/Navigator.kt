package com.example.screenmanager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList

/** Kako se došlo do trenutnog ekrana — određuje animaciju prelaza. */
enum class NavTransition { Push, Pop, SwitchTab }

/**
 * Jedini izvor istine za navigaciju: back-stack ekrana.
 *
 * Pravila:
 * - Koren stack-a je UVEK [Screen.Home] — jedini ekran sa kog "nazad" izlazi iz aplikacije.
 * - Izbor taba menja ceo stack u `[Home]` ili `[Home, tab]` (nazad sa taba vodi na Home).
 * - Ostali ekrani se dodaju na vrh ([push]) i skidaju sa [back].
 *
 * Sistemski back, swipe-back gest i strelica u naslovu zovu isti [back].
 */
@Stable
class Navigator internal constructor(private val backStack: SnapshotStateList<Screen>) {

    val current: Screen
        get() = backStack.last()

    /** Tab ispod trenutnog ekrana (označen u donjoj navigaciji). */
    val currentTab: Screen.Tab
        get() = backStack.last { it is Screen.Tab } as Screen.Tab

    val canGoBack: Boolean
        get() = backStack.size > 1

    /** Rute svih ekrana na stack-u (za čišćenje sačuvanog stanja zatvorenih ekrana). */
    val routes: List<String>
        get() = backStack.map { it.route }

    var lastTransition by mutableStateOf(NavTransition.SwitchTab)
        private set

    fun push(screen: Screen) {
        if (screen is Screen.Tab) return selectTab(screen)
        if (current == screen) return
        lastTransition = NavTransition.Push
        backStack.add(screen)
    }

    fun selectTab(tab: Screen.Tab) {
        if (current == tab) return
        lastTransition = NavTransition.SwitchTab
        backStack.removeRange(1, backStack.size)
        if (tab != Screen.Home) backStack.add(tab)
    }

    fun back() {
        if (!canGoBack) return
        lastTransition = if (current is Screen.Tab) NavTransition.SwitchTab else NavTransition.Pop
        backStack.removeAt(backStack.lastIndex)
    }
}

@Composable
fun rememberNavigator(): Navigator {
    val backStack = rememberSaveable(
        saver = listSaver<SnapshotStateList<Screen>, Screen>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) {
        mutableStateListOf<Screen>(Screen.Home)
    }
    return remember(backStack) { Navigator(backStack) }
}
