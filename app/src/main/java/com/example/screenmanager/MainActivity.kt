package com.example.screenmanager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.screenmanager.service.FocusMonitorService
import com.example.screenmanager.ui.ScreenManagerApp
import com.example.screenmanager.ui.theme.ScreenManagerTheme

/**
 * Ulazna Android aktivnost.
 *
 * Postavlja Compose temu i predaje kontrolu [ScreenManagerApp]. Na svakom
 * povratku u aplikaciju (onResume) pokušava da pokrene [FocusMonitorService]
 * — korisnik se tipično vraća iz sistemskih podešavanja posle davanja
 * Usage Access dozvole, pa servis kreće bez restarta aplikacije.
 */
class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        setContent {
            ScreenManagerTheme {
                ScreenManagerApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        FocusMonitorService.start(this)
    }

    /** Bez ove dozvole (API 33+) FGS notifikacija i alarmi nisu vidljivi. */
    private fun requestNotificationPermissionIfNeeded() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
