package com.underscore.app.context

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.scan

data class ContextShift(
    val from: SceneClassification,
    val to: SceneClassification,
    val isUrgent: Boolean
)

class ContextShiftDetector {

    // Transitions that warrant immediate music change (1s crossfade)
    private val urgentTransitions = setOf(
        SceneClassification.STATIONARY to SceneClassification.TRANSIT,
        SceneClassification.WALKING to SceneClassification.ACTIVE,
        SceneClassification.TRANSIT to SceneClassification.ACTIVE,
    )

    // Flatten stationary subtypes for urgency check
    private val SceneClassification.isStationary: Boolean
        get() = this in setOf(
            SceneClassification.MORNING_STATIONARY,
            SceneClassification.DAYTIME_STATIONARY,
            SceneClassification.EVENING_STATIONARY,
            SceneClassification.NIGHT_STATIONARY
        )

    private val SceneClassification.normalized: SceneClassification
        get() = if (isStationary) SceneClassification.MORNING_STATIONARY else this
        // Use MORNING_STATIONARY as generic "stationary" for urgency comparison

    fun detectShifts(classificationFlow: Flow<SceneClassification>): Flow<ContextShift> {
        return classificationFlow
            .scan<SceneClassification, Pair<SceneClassification?, SceneClassification?>>(
                null to null
            ) { prev, current ->
                prev.second to current
            }
            .filter { (prev, current) -> prev != null && current != null && prev != current }
            .map { (prev, current) ->
                val from = prev!!
                val to = current!!
                val normalizedPair = from.normalized to to.normalized
                ContextShift(
                    from = from,
                    to = to,
                    isUrgent = normalizedPair in urgentTransitions
                )
            }
    }

    // Extension to use map on Flow<Pair> — Kotlin's stdlib doesn't have it on pairs in Flow
    private fun <T, R> Flow<T>.map(transform: (T) -> R): Flow<R> =
        kotlinx.coroutines.flow.map(this, transform)
}
