package com.underscore.app.context

import java.time.Instant
import java.time.LocalTime

enum class LocationType {
    STATIONARY, WALKING, TRANSIT, UNKNOWN
}

enum class MovementIntensity {
    STILL, LIGHT, MODERATE, INTENSE
}

enum class TimeOfDay {
    MORNING,     // 06:00 - 11:59
    AFTERNOON,   // 12:00 - 16:59
    EVENING,     // 17:00 - 20:59
    NIGHT;       // 21:00 - 05:59

    companion object {
        fun fromLocalTime(time: LocalTime): TimeOfDay = when (time.hour) {
            in 6..11 -> MORNING
            in 12..16 -> AFTERNOON
            in 17..20 -> EVENING
            else -> NIGHT
        }
    }
}

enum class SceneClassification {
    MORNING_STATIONARY,
    DAYTIME_STATIONARY,
    EVENING_STATIONARY,
    NIGHT_STATIONARY,
    TRANSIT,
    WALKING,
    ACTIVE,
    UNKNOWN
}

data class SceneState(
    val timestamp: Instant = Instant.now(),
    val locationType: LocationType = LocationType.UNKNOWN,
    val speedKmh: Float = 0f,
    val movementIntensity: MovementIntensity = MovementIntensity.STILL,
    val timeOfDay: TimeOfDay = TimeOfDay.fromLocalTime(LocalTime.now()),
    val previousClassification: SceneClassification? = null,
    val minutesInCurrentScene: Int = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val weather: String? = null, // "clear", "rain", "storm", "clouds", etc.
    // World Layer — place/zone awareness
    val placeType: String? = null,           // "gym", "temple", "restaurant", etc.
    val zoneCharacter: String? = null,       // "commercial", "nightlife", "cultural", etc.
    val tonalPalette: String? = null,        // Musical descriptors for narrative engine
    val narrativeFunction: String? = null,   // What this place means in the protagonist's story
    val nearbyLandmarks: List<String> = emptyList()
)

data class SensorSnapshot(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speedMps: Float = 0f,
    val accelerometerMagnitude: Float = 0f,
    val timestamp: Instant = Instant.now()
)
