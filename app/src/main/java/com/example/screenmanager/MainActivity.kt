package com.example.screenmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.screenmanager.ui.ScreenManagerApp
import com.example.screenmanager.ui.theme.ScreenManagerTheme

/**
 * Ulazna Android aktivnost.
 *
 * Ovo je prvi UI korak nakon startovanja aplikacije: sistem ulazi u [onCreate],
 * aktivnost postavlja Compose temu i predaje kontrolu [ScreenManagerApp], koja
 * dalje bira početni ekran i navigaciju kroz celu aplikaciju.
 */
class MainActivity : ComponentActivity() {
    /**
     * Postavlja Compose temu i predaje kontrolu glavnom UI router-u.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScreenManagerTheme {
                ScreenManagerApp()
            }
        }
    }
}