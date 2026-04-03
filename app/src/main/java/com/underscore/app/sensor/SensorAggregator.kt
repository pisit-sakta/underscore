package com.underscore.app.sensor

import android.content.Context
import com.underscore.app.context.LocationType
import com.underscore.app.context.MovementIntensity
import com.underscore.app.context.SceneState
import com.underscore.app.context.TimeOfDay
import com.underscore.app.context.ZoneScorer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalTime

class SensorAggregator(
    context: Context,
    private val placesProvider: PlacesProvider? = null
) {

    private val locationProvider = LocationProvider(context)
    private val motionDetector = MotionDetector(context)
    private val zoneScorer = ZoneScorer()

    // Cache places result to avoid blocking the Flow on every emission
    private var cachedPlaces: PlacesResult? = null
    private var lastPlacesLat: Double = 0.0
    private var lastPlacesLng: Double = 0.0

    fun sceneStateFlow(): Flow<SceneState> {
        return combine(
            locationProvider.locationUpdates(),
            motionDetector.motionUpdates()
        ) { location, motion ->
            val speedKmh = location.speedMps * 3.6f
            val locationType = classifyLocation(speedKmh, motion.intensity)
            val timeOfDay = TimeOfDay.fromLocalTime(LocalTime.now())

            // Fetch places if moved >100m from last check (non-blocking cache)
            val places = fetchPlacesIfNeeded(location.latitude, location.longitude)
            val zone = zoneScorer.score(places, timeOfDay)

            SceneState(
                timestamp = Instant.now(),
                locationType = locationType,
                speedKmh = speedKmh,
                movementIntensity = motion.intensity,
                timeOfDay = timeOfDay,
                latitude = location.latitude,
                longitude = location.longitude,
                placeType = zone.placeType,
                zoneCharacter = zone.zoneCharacter,
                tonalPalette = zone.tonalPalette,
                narrativeFunction = zone.narrativeFunction,
                nearbyLandmarks = zone.nearbyLandmarks
            )
        }
    }

    private suspend fun fetchPlacesIfNeeded(lat: Double, lng: Double): PlacesResult? {
        val provider = placesProvider ?: return null
        // PlacesProvider handles its own distance-based caching internally
        val result = provider.getNearbyPlaces(lat, lng)
        if (result != null) {
            cachedPlaces = result
            lastPlacesLat = lat
            lastPlacesLng = lng
        }
        return cachedPlaces
    }

    private fun classifyLocation(
        speedKmh: Float,
        intensity: MovementIntensity
    ): LocationType = when {
        speedKmh > 15f -> LocationType.TRANSIT
        intensity == MovementIntensity.MODERATE || intensity == MovementIntensity.LIGHT -> LocationType.WALKING
        intensity == MovementIntensity.INTENSE -> LocationType.WALKING
        else -> LocationType.STATIONARY
    }
}
