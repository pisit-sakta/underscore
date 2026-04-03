package com.underscore.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.underscore.app.context.MovementIntensity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

data class MotionUpdate(
    val magnitude: Float,
    val intensity: MovementIntensity
)

class MotionDetector(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Rolling average to smooth out noise
    private val magnitudeBuffer = FloatArray(10)
    private var bufferIndex = 0
    private var bufferFull = false

    fun motionUpdates(): Flow<MotionUpdate> = callbackFlow {
        if (accelerometer == null) {
            // No accelerometer — emit STILL and stay open
            trySend(MotionUpdate(0f, MovementIntensity.STILL))
            awaitClose()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                // Subtract gravity (roughly 9.81) from total magnitude
                val totalMagnitude = sqrt(x * x + y * y + z * z)
                val dynamicMagnitude = kotlin.math.abs(totalMagnitude - SensorManager.GRAVITY_EARTH)

                magnitudeBuffer[bufferIndex] = dynamicMagnitude
                bufferIndex = (bufferIndex + 1) % magnitudeBuffer.size
                if (bufferIndex == 0) bufferFull = true

                val count = if (bufferFull) magnitudeBuffer.size else bufferIndex
                if (count == 0) return
                val avgMagnitude = magnitudeBuffer.take(count).average().toFloat()

                val intensity = classifyIntensity(avgMagnitude)
                trySend(MotionUpdate(avgMagnitude, intensity))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            listener,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL // ~200ms between updates
        )

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    private fun classifyIntensity(magnitude: Float): MovementIntensity = when {
        magnitude < 0.5f -> MovementIntensity.STILL
        magnitude < 2.0f -> MovementIntensity.LIGHT
        magnitude < 6.0f -> MovementIntensity.MODERATE
        else -> MovementIntensity.INTENSE
    }
}
