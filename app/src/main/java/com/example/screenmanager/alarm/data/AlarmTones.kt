package com.example.screenmanager.alarm.data

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri

/**
 * Zvuci alarma. `toneId` iz [com.example.screenmanager.model.AlarmSound]:
 * - `system` — podrazumevani sistemski zvuk alarma;
 * - `custom:<uri>` — fajl koji je korisnik izabrao.
 */
object AlarmTones {
    const val SYSTEM_ID = "system"
    const val CUSTOM_PREFIX = "custom:"

    /**
     * Redosled URI-ja koje plejer pokušava: izabrani zvuk, pa sistemski
     * alarm, pa sistemska melodija zvona. Alarm ne sme da ostane nem zbog
     * obrisanog fajla ili oduzete dozvole.
     */
    fun playbackCandidates(context: Context, toneId: String): List<Uri> {
        val chosen = resolve(context, toneId)
        val fallbacks = listOfNotNull(
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        )
        return (listOfNotNull(chosen) + fallbacks).distinct()
    }

    fun resolve(context: Context, toneId: String): Uri? = when {
        toneId.startsWith(CUSTOM_PREFIX) -> Uri.parse(toneId.removePrefix(CUSTOM_PREFIX))
        else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    }
}
