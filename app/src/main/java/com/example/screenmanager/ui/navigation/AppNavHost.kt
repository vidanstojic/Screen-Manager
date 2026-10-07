package com.example.screenmanager.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.screenmanager.ui.feature.alarms.AlarmEditorRoute
import com.example.screenmanager.ui.feature.alarms.AlarmsRoute
import com.example.screenmanager.ui.feature.appdetails.AppDetailsRoute
import com.example.screenmanager.ui.feature.home.HomeRoute
import com.example.screenmanager.ui.feature.limits.LimitsRoute
import com.example.screenmanager.ui.feature.limits.editor.RuleEditorRoute
import com.example.screenmanager.ui.feature.settings.SettingsRoute
import com.example.screenmanager.ui.feature.stats.StatsRoute
import com.example.screenmanager.ui.theme.AppTheme

/**
 * Koren UI-a: prikazuje ekran sa vrha back-stack-a i donju navigaciju.
 *
 * - Donja navigacija je vidljiva samo na tabovima.
 * - Ekrani otvoreni preko taba podržavaju swipe-back sa leve ivice.
 * - Stanje ekrana (skrol, izbor) se čuva dok je ekran na stack-u, a briše
 *   kad se ekran zatvori; tabovi svoje stanje zadržavaju stalno.
 */
@Composable
fun AppNavHost(navigator: Navigator = rememberNavigator()) {
    val stateHolder = rememberSaveableStateHolder()

    // Sistemski back; na Home je isključen pa Android sam izlazi iz aplikacije.
    BackHandler(enabled = navigator.canGoBack) { navigator.back() }

    // Zaboravi sačuvano stanje ekrana koji više nisu na stack-u (osim tabova).
    val routes = navigator.routes
    var knownRoutes by remember { mutableStateOf(routes) }
    LaunchedEffect(routes) {
        (knownRoutes - routes.toSet())
            .filterNot { it.startsWith(TAB_ROUTE_PREFIX) }
            .forEach(stateHolder::removeState)
        knownRoutes = routes
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        AnimatedContent(
            targetState = navigator.current,
            modifier = Modifier.weight(1f),
            transitionSpec = { screenTransition(navigator.lastTransition) },
            label = "screen"
        ) { screen ->
            stateHolder.SaveableStateProvider(screen.route) {
                if (screen is Screen.Tab) {
                    ScreenContent(screen = screen, navigator = navigator)
                } else {
                    SwipeBackContainer(onBack = navigator::back) {
                        ScreenContent(screen = screen, navigator = navigator)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = navigator.current is Screen.Tab,
            enter = slideInVertically(tween(220)) { it } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(180))
        ) {
            AppBottomBar(selected = navigator.currentTab, onSelect = navigator::selectTab)
        }
    }
}

/** Jedino mesto koje zna koji composable prikazuje koji [Screen]. */
@Composable
private fun ScreenContent(screen: Screen, navigator: Navigator) {
    when (screen) {
        Screen.Home -> HomeRoute(
            onOpenStats = { navigator.selectTab(Screen.Stats) },
            onOpenLimits = { navigator.selectTab(Screen.Limits) },
            onOpenAlarms = { navigator.selectTab(Screen.Alarms) },
            onOpenSettings = { navigator.push(Screen.Settings) },
            onOpenApp = { navigator.push(Screen.AppDetails(it.packageName, it.label)) }
        )

        Screen.Stats -> StatsRoute(
            onOpenApp = { navigator.push(Screen.AppDetails(it.packageName, it.label)) },
            onOpenSettings = { navigator.push(Screen.Settings) }
        )

        Screen.Limits -> LimitsRoute(
            onEditRule = { navigator.push(Screen.RuleEditor(it)) }
        )

        Screen.Alarms -> AlarmsRoute(
            onEditAlarm = { navigator.push(Screen.AlarmEditor(it)) }
        )

        is Screen.AppDetails -> AppDetailsRoute(
            packageName = screen.packageName,
            appName = screen.appName,
            onBack = navigator::back,
            onEditRule = { navigator.push(Screen.RuleEditor(it)) }
        )

        is Screen.RuleEditor -> RuleEditorRoute(target = screen.target, onDone = navigator::back)

        is Screen.AlarmEditor -> AlarmEditorRoute(alarmId = screen.alarmId, onDone = navigator::back)

        Screen.Settings -> SettingsRoute(onBack = navigator::back)
    }
}

private const val TAB_ROUTE_PREFIX = "tab/"

/** Animacija prelaza: tabovi se pretapaju, ekrani preko taba klize sa strane. */
private fun AnimatedContentTransitionScope<Screen>.screenTransition(kind: NavTransition): ContentTransform =
    when (kind) {
        NavTransition.SwitchTab ->
            fadeIn(tween(200, delayMillis = 60)) togetherWith fadeOut(tween(100))

        NavTransition.Push ->
            (slideInHorizontally(tween(260)) { it / 6 } + fadeIn(tween(260))) togetherWith
                fadeOut(tween(120))

        NavTransition.Pop ->
            (slideInHorizontally(tween(260)) { -it / 6 } + fadeIn(tween(260))) togetherWith
                (slideOutHorizontally(tween(200)) { it / 6 } + fadeOut(tween(140)))
    }
