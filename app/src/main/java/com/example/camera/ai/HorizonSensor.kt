package com.example.camera.ai

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Sensor-based Horizon Leveler.
 * Uses hardware accelerometer to calculate tilt angle.
 * Emits haptic tick when reaching level horizon.
 */
class HorizonSensor(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val _rollAngle = MutableStateFlow(0f)
    val rollAngle: StateFlow<Float> = _rollAngle.asStateFlow()

    private val _isLevel = MutableStateFlow(false)
    val isLevel: StateFlow<Boolean> = _isLevel.asStateFlow()

    private var wasLevel = false
    var hapticFeedbackEnabled: Boolean = true

    fun start() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val ax = event.values[0]
        val ay = event.values[1]

        // Calculate roll angle in degrees
        val angleRad = atan2(ax.toDouble(), ay.toDouble())
        val angleDeg = Math.toDegrees(angleRad).toFloat()

        // Normalize relative to 90 degrees (upright portrait orientation)
        val relativeRoll = if (angleDeg > 0) 90f - angleDeg else -90f - angleDeg
        _rollAngle.value = relativeRoll

        val currentlyLevel = abs(relativeRoll) <= 1.2f
        _isLevel.value = currentlyLevel

        if (currentlyLevel && !wasLevel && hapticFeedbackEnabled) {
            triggerLevelHaptic()
        }
        wasLevel = currentlyLevel
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun triggerLevelHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(15L)
            }
        } catch (_: Exception) {}
    }
}
