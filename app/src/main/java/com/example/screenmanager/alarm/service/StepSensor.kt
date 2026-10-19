package com.example.screenmanager.alarm.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.screenmanager.alarm.domain.StepValidator

/**
 * Brojanje koraka za misiju: [Sensor.TYPE_STEP_DETECTOR] (jedan događaj po koraku,
 * zahteva dozvolu ACTIVITY_RECOGNITION) + akcelerometar i žiroskop kojima [StepValidator]
 * odbacuje lažne korake (njihanje telefona, BUG-001/BUG-012). Odluka po koraku ide u
 * logcat (tag "StepMission") radi podešavanja pragova.
 */
class StepSensor(context: Context) {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(SensorManager::class.java)
    private val detector: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private var listener: SensorEventListener? = null

    val isAvailable: Boolean
        get() = detector != null && hasPermission(appContext)

    fun start(onStep: () -> Unit) {
        val stepSensor = detector ?: return
        stop()
        val validator = StepValidator { Log.d(TAG, it) }
        val newListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val time = event.timestamp / NANOS_PER_MS
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER ->
                        validator.onAcceleration(time, event.values[0], event.values[1], event.values[2])

                    Sensor.TYPE_GYROSCOPE ->
                        validator.onRotation(time, event.values[0], event.values[1], event.values[2])

                    Sensor.TYPE_STEP_DETECTOR -> {
                        val reported = event.values.firstOrNull()?.toInt()?.coerceAtLeast(1) ?: 1
                        repeat(reported) { repeat(validator.onStepEvent(time)) { onStep() } }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        listener = newListener
        accelerometer?.let { sensorManager.registerListener(newListener, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroscope?.let { sensorManager.registerListener(newListener, it, SensorManager.SENSOR_DELAY_GAME) }
        sensorManager.registerListener(newListener, stepSensor, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        listener?.let { sensorManager.unregisterListener(it) }
        listener = null
    }

    companion object {
        private const val NANOS_PER_MS = 1_000_000L
        private const val TAG = "StepMission"

        fun hasPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
                PackageManager.PERMISSION_GRANTED

        fun hasSensor(context: Context): Boolean =
            context.getSystemService(SensorManager::class.java)?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null
    }
}
