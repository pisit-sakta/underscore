package com.underscore.app.context

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

data class ContextShift(
    val from: SceneClassification,
    val to: SceneClassification,
    val isUrgent: Boolean
)

class ContextShiftDetector {

    private val stationaryTypes = setOf(
        SceneClassification.MORNING_STATIONARY,
        SceneClassification.DAYTIME_STATIONARY,
        SceneClassification.EVENING_STATIONARY,
        SceneClassification.NIGHT_STATIONARY
    )

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
                ContextShift(
                    from = from,
                    to = to,
                    isUrgent = isUrgentTransition(from, to)
                )
            }
    }

    private fun isUrgentTransition(from: SceneClassification, to: SceneClassification): Boolean {
        // Stationary -> Transit or any -> Active are urgent
        if (from in stationaryTypes && to == SceneClassification.TRANSIT) return true
        if (to == SceneClassification.ACTIVE) return true
        return false
    }
}
