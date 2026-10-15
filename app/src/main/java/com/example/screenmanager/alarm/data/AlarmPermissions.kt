package com.example.screenmanager.alarm.data

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.screenmanager.domain.PlatformCompat

/** Jedna dozvola bitna za alarme: da li je data i kako je korisnik daje. */
data class AlarmPermission(
    val kind: Kind,
    val granted: Boolean
) {
    enum class Kind(val title: String, val reason: String) {
        NOTIFICATIONS("Notifications", "Needed to show a ringing alarm."),
        EXACT_ALARMS("Alarms & reminders", "Lets alarms ring at the exact minute."),
        FULL_SCREEN("Full-screen alarm", "Shows the alarm screen over the lock screen."),
        DISPLAY_OVER_APPS("Display over other apps", "Opens the alarm screen while you use the phone.")
    }
}

/**
 * Stanje dozvola koje utiču na alarme. Bez njih alarm i dalje zvoni
 * (zvuk pušta servis), ali kasni, ne pokazuje ekran ili se ne vidi.
 */
class AlarmPermissions(private val context: Context) {

    fun snapshot(): List<AlarmPermission> = listOf(
        AlarmPermission(AlarmPermission.Kind.NOTIFICATIONS, notificationsGranted()),
        AlarmPermission(
            AlarmPermission.Kind.EXACT_ALARMS,
            PlatformCompat.canScheduleExactAlarms(context.getSystemService(AlarmManager::class.java))
        ),
        AlarmPermission(
            AlarmPermission.Kind.FULL_SCREEN,
            PlatformCompat.canUseFullScreenIntent(context.getSystemService(NotificationManager::class.java))
        ),
        AlarmPermission(AlarmPermission.Kind.DISPLAY_OVER_APPS, Settings.canDrawOverlays(context))
    )

    /** Sistemski ekran na kom korisnik daje dozvolu [kind]. */
    fun settingsIntent(kind: AlarmPermission.Kind): Intent {
        val packageUri = Uri.parse("package:${context.packageName}")
        return when (kind) {
            AlarmPermission.Kind.NOTIFICATIONS ->
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            // Ove dve stavke su nedozvoljene samo na verzijama koje imaju i ova podešavanja (31+ / 34+).
            AlarmPermission.Kind.EXACT_ALARMS ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri)
                } else {
                    appDetails(packageUri)
                }
            AlarmPermission.Kind.FULL_SCREEN ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri)
                } else {
                    appDetails(packageUri)
                }
            AlarmPermission.Kind.DISPLAY_OVER_APPS -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun notificationsGranted(): Boolean = PlatformCompat.notificationsGranted(context)

    private fun appDetails(packageUri: Uri) = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
}
