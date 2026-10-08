package com.example.screenmanager.alarm.data

import android.content.ContentResolver
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.annotation.RawRes
import com.example.screenmanager.R
import com.example.screenmanager.model.AlarmSound

/** Zvuk koji dolazi sa aplikacijom (res/raw, generisan za projekat — bez licenci trećih strana). */
data class BuiltinTone(val id: String, val name: String, val description: String, @RawRes val resId: Int)

/**
 * Katalog i razrešavanje zvukova alarma. `toneId` iz [AlarmSound]:
 * - `builtin:<ime>` — zvuk aplikacije ([builtins]);
 * - `system` — podrazumevani sistemski zvuk alarma;
 * - `custom:<uri>` — fajl koji je korisnik izabrao (trajna dozvola za čitanje URI-ja).
 */
object AlarmTones {
    const val BUILTIN_PREFIX = "builtin:"
    const val SYSTEM_ID = "system"
    const val CUSTOM_PREFIX = "custom:"

    val builtins: List<BuiltinTone> = listOf(
        BuiltinTone("builtin:sunrise", "Sunrise", "Soft rising bells", R.raw.alarm_sunrise),
        BuiltinTone("builtin:marimba", "Marimba", "Warm wooden melody", R.raw.alarm_marimba),
        BuiltinTone("builtin:chimes", "Wind chimes", "Calm, airy chimes", R.raw.alarm_chimes),
        BuiltinTone("builtin:birds", "Morning birds", "Bright chirping", R.raw.alarm_birds),
        BuiltinTone("builtin:pulse", "Pulse", "Pulses that speed up", R.raw.alarm_pulse),
        BuiltinTone("builtin:classic", "Classic beep", "Loud digital beeps", R.raw.alarm_classic)
    )

    fun customId(uri: Uri): String = CUSTOM_PREFIX + uri

    /** Ime zvuka za prikaz u formi i listi. */
    fun displayName(sound: AlarmSound): String = when {
        sound.toneId == SYSTEM_ID -> "System default"
        sound.toneId.startsWith(CUSTOM_PREFIX) -> sound.customName ?: "Your sound"
        else -> builtins.firstOrNull { it.id == sound.toneId }?.name ?: builtins.first().name
    }

    /**
     * Redosled URI-ja koje plejer pokušava: izabrani zvuk, pa podrazumevani
     * zvuk aplikacije, pa sistemski alarm i melodija zvona. Alarm ne sme da
     * ostane nem zbog obrisanog fajla ili oduzete dozvole.
     */
    fun playbackCandidates(context: Context, toneId: String): List<Uri> {
        val fallbacks = listOfNotNull(
            rawUri(context, builtins.first().resId),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        )
        return (listOfNotNull(resolve(context, toneId)) + fallbacks).distinct()
    }

    fun resolve(context: Context, toneId: String): Uri? = when {
        toneId == SYSTEM_ID -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        toneId.startsWith(CUSTOM_PREFIX) -> Uri.parse(toneId.removePrefix(CUSTOM_PREFIX))
        else -> builtins.firstOrNull { it.id == toneId }?.let { rawUri(context, it.resId) }
    }

    private fun rawUri(context: Context, @RawRes resId: Int): Uri =
        Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/$resId")
}
