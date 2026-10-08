package com.example.screenmanager.alarm.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat

/**
 * Brojanje koraka za misiju ([Sensor.TYPE_STEP_DETECTOR]: jedan događaj po
 * koraku, sa malim kašnjenjem). Zahteva dozvolu ACTIVITY_RECOGNITION.
 */
class StepSensor(context: Context) {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(SensorManager::class.java)
    private val detector: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private var listener: SensorEventListener? = null

    val isAvailable: Boolean
        get() = detector != null && hasPermission(appContext)

    fun start(onStep: () -> Unit) {
        val sensor = detector ?: return
        stop()
        val newListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                repeat(event.values.firstOrNull()?.toInt()?.coerceAtLeast(1) ?: 1) { onStep() }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        listener = newListener
        sensorManager.registerListener(newListener, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        listener?.let { sensorManager.unregisterListener(it) }
        listener = null
    }

    companion object {
        fun hasPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
                PackageManager.PERMISSION_GRANTED

        fun hasSensor(context: Context): Boolean =
            context.getSystemService(SensorManager::class.java)?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null
    }
}
