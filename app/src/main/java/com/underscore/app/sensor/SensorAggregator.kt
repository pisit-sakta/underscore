package com.underscore.app.sensor

import android.content.Context
import com.underscore.app.context.LocationType
import com.underscore.app.context.MovementIntensity
import com.underscore.app.context.SceneState
import com.underscore.app.context.TimeOfDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalTime

class SensorAggregator(context: Context) {

    private val locationProvider = LocationProvider(context)
    private val motionDetector = MotionDetector(context)

    fun sceneStateFlow(): Flow<SceneState> {
        return combine(
            locationProvider.locationUpdates(),
            motionDetector.motionUpdates()
        ) { location, motion ->
            val speedKmh = location.speedMps * 3.6f
            val locationType = classifyLocation(speedKmh, motion.intensity)

            SceneState(
                timestamp = Instant.now(),
                locationType = locationType,
                speedKmh = speedKmh,
                movementIntensity = motion.intensity,
                timeOfDay = TimeOfDay.fromLocalTime(LocalTime.now())
            )
        }
    }

    private fun classifyLocation(
        speedKmh: Float,
        intensity: MovementIntensity
    ): LocationType = when {
        speedKmh > 15f -> LocationType.TRANSIT
        intensity == MovementIntensity.MODERATE || intensity == MovementIntensity.LIGHT -> LocationType.WALKING
        intensity == MovementIntensity.INTENSE -> LocationType.WALKING // Could be running
        else -> LocationType.STATIONARY
    }
}
