package com.underscore.app.playback

import com.underscore.app.debug.AppLog
import com.underscore.app.context.ContextShift
import kotlinx.coroutines.delay

class TransitionManager(private val playbackController: PlaybackController) {

    companion object {
        private const val TAG = "TransitionManager"
        private const val NORMAL_CROSSFADE_MS = 3000L
        private const val URGENT_CROSSFADE_MS = 1000L
        private const val DRAMATIC_SILENCE_MS = 500L
    }

    suspend fun transition(newTrackUri: String, shift: ContextShift?) {
        val currentTrack = playbackController.nowPlaying.value.trackUri

        // Don't transition to the same track
        if (currentTrack == newTrackUri) {
            AppLog.d(TAG, "Already playing $newTrackUri — skipping transition")
            return
        }

        val isUrgent = shift?.isUrgent ?: false

        if (isUrgent) {
            AppLog.d(TAG, "Urgent transition -> $newTrackUri")
            // Quick crossfade: just play immediately (Spotify handles its own crossfade)
            playbackController.playTrack(newTrackUri)
        } else {
            AppLog.d(TAG, "Normal transition -> $newTrackUri")
            // For now, just play the new track — Spotify's built-in crossfade handles the rest.
            // In Sprint 2, we can add volume ramping via Web API for smoother control.
            playbackController.playTrack(newTrackUri)
        }
    }

    suspend fun dramaticSilence(newTrackUri: String) {
        AppLog.d(TAG, "Dramatic silence before $newTrackUri")
        playbackController.pause()
        delay(DRAMATIC_SILENCE_MS)
        playbackController.playTrack(newTrackUri)
    }
}
