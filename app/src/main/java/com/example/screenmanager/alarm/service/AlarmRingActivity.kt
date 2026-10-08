package com.example.screenmanager.alarm.service

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.example.screenmanager.ui.feature.alarms.ring.AlarmRingRoute
import com.example.screenmanager.ui.theme.AppSection
import com.example.screenmanager.ui.theme.AuroraBackground
import com.example.screenmanager.ui.theme.ScreenManagerTheme
import com.example.screenmanager.ui.theme.SectionTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Ekran alarma koji zvoni. Prikazuje se preko zaključanog ekrana i pali
 * ekran. Nema "nazad": alarm se gasi samo dugmetom (ili rešenom misijom)
 * ili odlaže. Zatvara se sam čim zvonjenje prestane (bilo kojim putem).
 */
class AlarmRingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })

        // Full-screen notifikacija može da otvori ekran pre nego što servis
        // učita alarm iz baze — sačekaj kratko, pa se zatvori kad zvonjenje prestane.
        lifecycleScope.launch {
            val started = withTimeoutOrNull(START_TIMEOUT_MS) { AlarmRingSession.active.first { it != null } }
            if (started != null) AlarmRingSession.active.first { it == null }
            finishAndRemoveTask()
        }

        setContent {
            val ring by AlarmRingSession.active.collectAsState()
            ScreenManagerTheme {
                SectionTheme(AppSection.Alarms) {
                    val active = ring
                    if (active == null) {
                        AuroraBackground {}
                    } else {
                        AlarmRingRoute(
                            ring = active,
                            onSnooze = { AlarmRingService.command(this, AlarmRingService.ACTION_SNOOZE) },
                            onDismiss = { AlarmRingService.command(this, AlarmRingService.ACTION_DISMISS) },
                            onMissionInteraction = { AlarmRingSession.reportMissionInteraction() }
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val START_TIMEOUT_MS = 5_000L
    }
}
