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
 * - Koren stack-a je UVEK [Screen.Launcher] — jedini ekran sa kog "nazad" izlazi iz aplikacije.
 * - Iznad korena je najviše jedan "ulaz u deo aplikacije": tab Screen Manager-a
 *   ili [Screen.Alarms]. Tabovi se međusobno ZAMENJUJU (ne slažu se), pa
 *   "nazad" sa bilo kog taba vodi na početni ekran.
 * - Ostali ekrani (detalji, forme) se dodaju na vrh ([push]) i skidaju sa [back].
 *
 * Sistemski back, swipe-back gest i strelica u naslovu zovu isti [back].
 */
@Stable
class Navigator internal constructor(private val backStack: SnapshotStateList<Screen>) {

    val current: Screen
        get() = backStack.last()

    /** Tab Screen Manager-a koji je na stack-u, ili null van Screen Manager-a. */
    val currentTab: Screen.Tab?
        get() = backStack.lastOrNull { it is Screen.Tab } as Screen.Tab?

    val canGoBack: Boolean
        get() = backStack.size > 1

    /** Rute svih ekrana na stack-u (za čišćenje sačuvanog stanja zatvorenih ekrana). */
    val routes: List<String>
        get() = backStack.map { it.route }

    var lastTransition by mutableStateOf(NavTransition.Push)
        private set

    fun push(screen: Screen) {
        if (screen is Screen.Tab) return selectTab(screen)
        if (current == screen) return
        lastTransition = NavTransition.Push
        backStack.add(screen)
    }

    /** Ulazak u Screen Manager (sa početnog ekrana) ili prelazak na drugi tab. */
    fun selectTab(tab: Screen.Tab) {
        if (current == tab) return
        val alreadyInScreenManager = currentTab != null
        lastTransition = if (alreadyInScreenManager) NavTransition.SwitchTab else NavTransition.Push
        backStack.removeRange(1, backStack.size)
        backStack.add(tab)
    }

    fun back() {
        if (!canGoBack) return
        lastTransition = NavTransition.Pop
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
        mutableStateListOf<Screen>(Screen.Launcher)
    }
    return remember(backStack) { Navigator(backStack) }
}
