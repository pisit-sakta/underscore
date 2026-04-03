package com.underscore.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class HeartRateUpdate(
    val bpm: Int,
    val heartRateState: HeartRateState,
    val timestamp: Long = System.currentTimeMillis()
)

enum class HeartRateState {
    RESTING,          // <70 bpm
    CALM,             // 70-90 bpm
    ELEVATED,         // 90-120 bpm
    COMBAT_ELEVATED,  // 120-150 bpm
    MAXIMUM;          // >150 bpm

    companion object {
        fun fromBpm(bpm: Int): HeartRateState = when {
            bpm < 70 -> RESTING
            bpm < 90 -> CALM
            bpm < 120 -> ELEVATED
            bpm < 150 -> COMBAT_ELEVATED
            else -> MAXIMUM
        }
    }
}

class HeartRateProvider(context: Context) {

    companion object {
        private const val TAG = "HeartRateProvider"
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val heartRateSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

    private var lastBpm: Int = 0
    private var lastState: HeartRateState = HeartRateState.RESTING

    val isAvailable: Boolean get() = heartRateSensor != null

    fun heartRateUpdates(): Flow<HeartRateUpdate> = callbackFlow {
        if (heartRateSensor == null) {
            Log.w(TAG, "No heart rate sensor available (no Wear OS or sensor)")
            // Emit a default resting state so the flow doesn't just hang
            trySend(HeartRateUpdate(bpm = 0, heartRateState = HeartRateState.RESTING))
            awaitClose {}
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_HEART_RATE && event.values.isNotEmpty()) {
                    val bpm = event.values[0].toInt()
                    if (bpm > 0) {
                        lastBpm = bpm
                        lastState = HeartRateState.fromBpm(bpm)
                        trySend(HeartRateUpdate(
                            bpm = bpm,
                            heartRateState = lastState
                        ))
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            listener,
            heartRateSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        Log.d(TAG, "Heart rate sensor registered")

        awaitClose {
            sensorManager.unregisterListener(listener)
            Log.d(TAG, "Heart rate sensor unregistered")
        }
    }

    fun getLastBpm(): Int = lastBpm
    fun getLastState(): HeartRateState = lastState
}
