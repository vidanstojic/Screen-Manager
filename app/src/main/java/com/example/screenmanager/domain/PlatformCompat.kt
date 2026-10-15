package com.example.screenmanager.domain

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat

/**
 * Pozivi koji ne postoje na svim verzijama od minSdk 29 (Android 10) naviše.
 * Jedno mesto za proveru verzije, da ostatak koda ne mora da je ponavlja.
 */
object PlatformCompat {
    /** Do Androida 12 (API 31) tačni alarmi su uvek dozvoljeni. */
    fun canScheduleExactAlarms(alarmManager: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** Do Androida 14 (API 34) full-screen notifikacije su uvek dozvoljene. */
    fun canUseFullScreenIntent(notificationManager: NotificationManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || notificationManager.canUseFullScreenIntent()

    /** Do Androida 13 (API 33) notifikacije ne traže dozvolu. */
    fun notificationPermissionNeeded(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun notificationsGranted(context: Context): Boolean =
        !notificationPermissionNeeded() ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun defaultVibrator(context: Context): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    /** Vibracija kao alarm (prolazi kroz "Ne uznemiravaj" kao i zvuk alarma). */
    fun vibrateAsAlarm(vibrator: Vibrator, effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
        }
    }
}
