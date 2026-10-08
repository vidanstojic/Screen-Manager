package com.example.screenmanager.alarm.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.content.Intent
import android.util.Log

/**
 * Kratko puštanje zvuka u formi alarma. Ide preko MEDIA toka (jačina
 * medija), da proba ne bi treštala punom jačinom alarma.
 */
class TonePreview(private val context: Context) {
    private var player: MediaPlayer? = null

    fun play(toneId: String) {
        stop()
        val uri = AlarmTones.resolve(context, toneId) ?: return
        val candidate = MediaPlayer()
        try {
            candidate.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            candidate.setDataSource(context, uri)
            candidate.setOnCompletionListener { stop() }
            candidate.prepare()
            candidate.start()
            player = candidate
        } catch (error: Exception) {
            Log.w("TonePreview", "Cannot preview $uri", error)
            candidate.release()
        }
    }

    fun stop() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
    }
}

/** Fajl koji je korisnik izabrao kao zvuk alarma. */
data class CustomTone(val toneId: String, val name: String)

object CustomTones {
    /**
     * Zadržava trajnu dozvolu za čitanje [uri] (inače bi istekla posle
     * restarta) i vraća id + ime fajla za prikaz.
     */
    fun import(context: Context, uri: Uri): CustomTone {
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.onFailure { Log.w("CustomTones", "No persistable permission for $uri", it) }
        val name = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
        return CustomTone(AlarmTones.customId(uri), name?.substringBeforeLast('.') ?: "Your sound")
    }
}
