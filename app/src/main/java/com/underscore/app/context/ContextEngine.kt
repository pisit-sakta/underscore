package com.underscore.app.context

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.map
import java.time.Instant

data class ClassifiedScene(
    val classification: SceneClassification,
    val sceneState: SceneState,
    val minutesInScene: Int,
    val sceneStartedAt: Instant
)

class ContextEngine {

    companion object {
        private const val TAG = "ContextEngine"
        private const val DEBOUNCE_SECONDS = 15L
    }

    fun classify(sceneStateFlow: Flow<SceneState>): Flow<ClassifiedScene> {
        return sceneStateFlow
            .scan(
                ClassifiedScene(
                    classification = SceneClassification.UNKNOWN,
                    sceneState = SceneState(),
                    minutesInScene = 0,
                    sceneStartedAt = Instant.now()
                )
            ) { previous, state ->
                val rawClassification = classifyScene(state)
                val now = Instant.now()
                val secondsInScene = now.epochSecond - previous.sceneStartedAt.epochSecond

                // Debounce: only accept a new classification if we've been in the
                // current one for at least DEBOUNCE_SECONDS, OR if it's a high-priority
                // transition (urgent shifts like sudden sprint always go through)
                val isUrgent = isUrgentShift(previous.classification, rawClassification)
                val shouldSwitch = rawClassification != previous.classification &&
                        (isUrgent || secondsInScene >= DEBOUNCE_SECONDS)

                if (shouldSwitch) {
                    Log.d(TAG, "Scene shift: ${previous.classification} -> $rawClassification" +
                            " (urgent=$isUrgent, after ${secondsInScene}s)" +
                            if (state.placeType != null) " @ ${state.placeType}" else "")
                    ClassifiedScene(
                        classification = rawClassification,
                        sceneState = state.copy(
                            previousClassification = previous.classification,
                            minutesInCurrentScene = 0
                        ),
                        minutesInScene = 0,
                        sceneStartedAt = now
                    )
                } else {
                    val minutes = (secondsInScene / 60).toInt()
                    ClassifiedScene(
                        classification = previous.classification,
                        sceneState = state.copy(
                            previousClassification = previous.classification,
                            minutesInCurrentScene = minutes
                        ),
                        minutesInScene = minutes,
                        sceneStartedAt = previous.sceneStartedAt
                    )
                }
            }
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

    private fun isUrgentShift(
        from: SceneClassification,
        to: SceneClassification
    ): Boolean {
        // These transitions should never be debounced
        val stationaryTypes = setOf(
            SceneClassification.MORNING_STATIONARY,
            SceneClassification.DAYTIME_STATIONARY,
            SceneClassification.EVENING_STATIONARY,
            SceneClassification.NIGHT_STATIONARY
        )
        // Stationary → Transit (departure!) or anything → Active (explosive sprint)
        if (from in stationaryTypes && to == SceneClassification.TRANSIT) return true
        if (to == SceneClassification.ACTIVE) return true
        return false
    }
}
