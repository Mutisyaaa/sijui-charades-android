package com.sijui.charades.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlin.math.abs

enum class TiltAction {
    CORRECT,
    PASS,
}

/**
 * Forehead tilt detector for Classic Charades.
 *
 * When the phone is held vertically against the forehead facing friends:
 *  - z ~ 0: neutral
 *  - z > +6.0 m/s^2: tilted backward / up toward the ceiling -> CORRECT
 *  - z < -6.0 m/s^2: tilted forward / down toward the floor -> PASS
 *
 * Requires returning to the neutral zone (|z| < 3.2) plus a cooldown so one tilt
 * never double-triggers.
 */
class TiltDetector(
    context: Context,
    private val onTilt: (TiltAction) -> Unit,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var armed = true
    private var lastTriggerMs = 0L

    fun start() {
        armed = true
        lastTriggerMs = SystemClock.elapsedRealtime()
        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val z = event.values[2]
        val now = SystemClock.elapsedRealtime()

        if (!armed) {
            if (abs(z) < 3.2f) {
                armed = true
            }
            return
        }

        if (now - lastTriggerMs < 700L) return

        when {
            z > 6.0f -> {
                armed = false
                lastTriggerMs = now
                onTilt(TiltAction.CORRECT)
            }
            z < -6.0f -> {
                armed = false
                lastTriggerMs = now
                onTilt(TiltAction.PASS)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
