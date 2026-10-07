package com.example.screenmanager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.screenmanager.service.FocusMonitorService
import com.example.screenmanager.ui.ScreenManagerApp

/**
 * Ulazna Android aktivnost.
 *
 * Predaje kontrolu [ScreenManagerApp] (tema + navigacija). Na svakom
 * povratku u aplikaciju (onResume) pokušava da pokrene [FocusMonitorService]
 * — korisnik se tipično vraća iz sistemskih podešavanja posle davanja
 * Usage Access dozvole, pa servis kreće bez restarta aplikacije.
 */
class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Tema je uvek tamna: sistemske trake su providne, sa svetlim ikonicama.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        setContent { ScreenManagerApp() }
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
