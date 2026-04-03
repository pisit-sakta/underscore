package com.underscore.app.context

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ContextEngine {

    fun classify(sceneStateFlow: Flow<SceneState>): Flow<SceneClassification> {
        return sceneStateFlow
            .map { state -> classifyScene(state) }
            .distinctUntilChanged() // Only emit when classification CHANGES
    }

    private fun classifyScene(state: SceneState): SceneClassification {
        // Priority 1: Speed-based transit detection
        if (state.speedKmh > 20f || state.locationType == LocationType.TRANSIT) {
            return SceneClassification.TRANSIT
        }

        // Priority 2: Intense physical activity
        if (state.movementIntensity == MovementIntensity.INTENSE) {
            return SceneClassification.ACTIVE
        }

        // Priority 3: Walking
        if (state.locationType == LocationType.WALKING) {
            return SceneClassification.WALKING
        }

        // Priority 4: Stationary — subdivide by time of day
        return when (state.timeOfDay) {
            TimeOfDay.MORNING -> SceneClassification.MORNING_STATIONARY
            TimeOfDay.AFTERNOON -> SceneClassification.DAYTIME_STATIONARY
            TimeOfDay.EVENING -> SceneClassification.EVENING_STATIONARY
            TimeOfDay.NIGHT -> SceneClassification.NIGHT_STATIONARY
        }
    }
}
