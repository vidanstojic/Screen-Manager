package com.example.screenmanager.alarm.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.PowerManager
import android.os.VibrationEffect
import android.util.Log
import com.example.screenmanager.alarm.data.AlarmTones
import com.example.screenmanager.alarm.domain.RingPolicy
import com.example.screenmanager.domain.PlatformCompat
import com.example.screenmanager.model.AlarmSound

/**
 * Zvuk i vibracija alarma koji zvoni.
 *
 * - Pušta preko ALARM toka ([AudioAttributes.USAGE_ALARM]) — čuje se i kad
 *   je telefon na nečujnom, a "Do not disturb" ga propušta ako su alarmi dozvoljeni.
 * - Za vreme zvonjenja postavlja jačinu ALARM toka na [AlarmSound.volumePercent],
 *   a posle vraća korisnikovu.
 * - Postepeno pojačavanje i utišavanje tokom misije radi pozivalac preko [setVolumeFraction].
 *
 * Nije thread-safe: poziva se samo sa main thread-a servisa.
 */
class AlarmPlayer(private val context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val vibrator = PlatformCompat.defaultVibrator(context)
    private var player: MediaPlayer? = null
    private var savedStreamVolume: Int? = null

    fun start(sound: AlarmSound) {
        stop()
        raiseStreamVolume(sound.volumePercent)
        player = createPlayer(sound.toneId)
        setVolumeFraction(RingPolicy.volumeFraction(0, sound.fadeInSeconds))
        player?.start()
        if (sound.vibrate) startVibration()
    }

    /** 0..1 deo jačine ALARM toka (postepeno pojačavanje, utišavanje tokom misije). */
    fun setVolumeFraction(fraction: Float) {
        val value = fraction.coerceIn(0f, 1f)
        runCatching { player?.setVolume(value, value) }
    }

    fun stop() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
        vibrator.cancel()
        savedStreamVolume?.let { runCatching { audioManager.setStreamVolume(AudioManager.STREAM_ALARM, it, 0) } }
        savedStreamVolume = null
    }

    private fun createPlayer(toneId: String): MediaPlayer? {
        for (uri in AlarmTones.playbackCandidates(context, toneId)) {
            val candidate = MediaPlayer()
            try {
                candidate.setAudioAttributes(ALARM_AUDIO)
                candidate.setDataSource(context, uri)
                candidate.isLooping = true
                candidate.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                candidate.prepare()
                return candidate
            } catch (error: Exception) {
                // Obrisan fajl, oduzeta dozvola za URI, neispravan format... → sledeći kandidat.
                Log.w(TAG, "Cannot play $uri, trying fallback", error)
                candidate.release()
            }
        }
        Log.e(TAG, "No playable alarm sound, vibration only")
        return null
    }

    private fun raiseStreamVolume(percent: Int) {
        runCatching {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            savedStreamVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, RingPolicy.streamVolumeIndex(percent, max), 0)
        }.onFailure { Log.w(TAG, "Cannot set alarm volume", it) }
    }

    private fun startVibration() {
        val effect = VibrationEffect.createWaveform(VIBRATION_PATTERN, 0)
        PlatformCompat.vibrateAsAlarm(vibrator, effect)
    }

    private companion object {
        const val TAG = "AlarmPlayer"
        val VIBRATION_PATTERN = longArrayOf(0, 700, 500, 700, 1_200)
        val ALARM_AUDIO: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }
}
