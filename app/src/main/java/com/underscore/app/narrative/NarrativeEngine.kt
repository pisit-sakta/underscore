package com.underscore.app.narrative

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.underscore.app.api.GeminiApi
import com.underscore.app.context.SceneClassification
import com.underscore.app.context.SceneState
import com.underscore.app.data.SongDatabase
import com.underscore.app.data.TaggedSong

data class SongSelection(
    val spotifyUri: String,
    val title: String,
    val artist: String,
    val matchReason: String,
    val transitionType: String, // "normal", "urgent", "dramatic_silence"
    val transitionDurationMs: Long
)

data class GeminiSongSelection(
    val spotify_uri: String,
    val title: String,
    val artist: String,
    val match_reason: String,
    val transition_type: String,
    val transition_duration_ms: Long
)

class NarrativeEngine(
    private val geminiApi: GeminiApi,
    private val db: SongDatabase
) {
    companion object {
        private const val TAG = "NarrativeEngine"
        private const val CANDIDATE_POOL_SIZE = 20
        private const val RECENTLY_PLAYED_WINDOW_MS = 30 * 60 * 1000L // 30 min
    }

    private val gson = Gson()
    private val fallbackSelector = SongSelector() // Sprint 0 fallback

    suspend fun selectSong(
        sceneState: SceneState,
        classification: SceneClassification,
        weather: String? = null
    ): SongSelection {
        val allSongs = db.taggedSongDao().getAll()

        // If library not yet analyzed, fall back to hardcoded selector
        if (allSongs.size < 10) {
            Log.d(TAG, "Library not analyzed yet, using fallback selector")
            val track = fallbackSelector.selectTrack(classification)
            return SongSelection(
                spotifyUri = track.uri,
                title = track.title,
                artist = track.artist,
                matchReason = "Fallback: library not yet analyzed",
                transitionType = "normal",
                transitionDurationMs = 3000
            )
        }

        // Get recently played to avoid repeats
        val recentUris = db.taggedSongDao().getRecentlyPlayedUris(
            System.currentTimeMillis() - RECENTLY_PLAYED_WINDOW_MS
        )

        // Pre-filter candidates by scene type relevance
        val candidates = preselectCandidates(allSongs, classification, recentUris)

        if (candidates.isEmpty()) {
            Log.w(TAG, "No candidates after filtering, using random")
            val random = allSongs.filter { it.spotifyUri !in recentUris }.randomOrNull()
                ?: allSongs.random()
            return SongSelection(
                spotifyUri = random.spotifyUri,
                title = random.title,
                artist = random.artist,
                matchReason = "Random selection (no candidates matched)",
                transitionType = "normal",
                transitionDurationMs = 3000
            )
        }

        // Build scene description for Gemini
        val sceneDescription = buildSceneDescription(sceneState, classification, weather)

        // Build track summaries for prompt
        val trackSummaries = candidates.map { song ->
            TaggedTrackSummary(
                uri = song.spotifyUri,
                title = song.title.replace("\"", "'"),
                artist = song.artist.replace("\"", "'"),
                sceneTypes = song.sceneTypes,
                energyCurve = song.energyCurve,
                emotionalRegister = song.emotionalRegister,
                bestFor = song.bestFor.replace("\"", "'")
            )
        }

        val prompt = Prompts.buildScoringPrompt(sceneDescription, trackSummaries, recentUris.take(5))
        val response = geminiApi.generate(
            prompt = prompt,
            systemPrompt = Prompts.SCENE_SCORER,
            temperature = 0.6f,
            maxTokens = 512,
            jsonMode = true
        )

        if (response != null) {
            try {
                val selection = gson.fromJson(response, GeminiSongSelection::class.java)
                // Record the play
                db.taggedSongDao().recordPlay(selection.spotify_uri)

                return SongSelection(
                    spotifyUri = selection.spotify_uri,
                    title = selection.title,
                    artist = selection.artist,
                    matchReason = selection.match_reason,
                    transitionType = selection.transition_type,
                    transitionDurationMs = selection.transition_duration_ms
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse Gemini selection", e)
            }
        }

        // Fallback: pick best candidate locally
        val pick = candidates.random()
        db.taggedSongDao().recordPlay(pick.spotifyUri)
        return SongSelection(
            spotifyUri = pick.spotifyUri,
            title = pick.title,
            artist = pick.artist,
            matchReason = "Local selection (Gemini unavailable)",
            transitionType = "normal",
            transitionDurationMs = 3000
        )
    }

    private fun preselectCandidates(
        allSongs: List<TaggedSong>,
        classification: SceneClassification,
        recentUris: List<String>
    ): List<TaggedSong> {
        val sceneKeywords = classificationToKeywords(classification)

        // Score each song by how many scene keywords match
        val scored = allSongs
            .filter { it.spotifyUri !in recentUris }
            .map { song ->
                val songSceneTypes: List<String> = try {
                    gson.fromJson(song.sceneTypes, object : TypeToken<List<String>>() {}.type)
                } catch (e: Exception) { emptyList() }

                val matchScore = songSceneTypes.count { sceneType ->
                    sceneKeywords.any { keyword -> sceneType.contains(keyword, ignoreCase = true) }
                }

                // Also factor in energy matching
                val energyScore = when (classification) {
                    SceneClassification.ACTIVE -> if (song.energy > 0.7f) 2 else 0
                    SceneClassification.TRANSIT -> if (song.energy in 0.4f..0.8f) 1 else 0
                    SceneClassification.NIGHT_STATIONARY -> if (song.energy < 0.4f) 2 else 0
                    SceneClassification.EVENING_STATIONARY -> if (song.energy < 0.5f) 1 else 0
                    SceneClassification.MORNING_STATIONARY -> if (song.valence > 0.3f) 1 else 0
                    else -> 0
                }

                song to (matchScore + energyScore)
            }
            .sortedByDescending { it.second }
            .take(CANDIDATE_POOL_SIZE)
            .map { it.first }

        return scored
    }

    private fun classificationToKeywords(classification: SceneClassification): List<String> = when (classification) {
        SceneClassification.MORNING_STATIONARY -> listOf("morning", "readiness", "awakening", "ominous")
        SceneClassification.DAYTIME_STATIONARY -> listOf("daytime", "work", "focus", "ambient", "contemplative")
        SceneClassification.EVENING_STATIONARY -> listOf("evening", "wind_down", "reflective", "calm")
        SceneClassification.NIGHT_STATIONARY -> listOf("night", "introspective", "serene", "melancholic")
        SceneClassification.TRANSIT -> listOf("transit", "departure", "journey", "movement", "driving", "riding")
        SceneClassification.WALKING -> listOf("walking", "stroll", "contemplative", "ambient")
        SceneClassification.ACTIVE -> listOf("active", "confrontation", "battle", "intense", "victory", "endurance")
        SceneClassification.UNKNOWN -> listOf("ambient", "neutral")
    }

    private fun buildSceneDescription(
        state: SceneState,
        classification: SceneClassification,
        weather: String?
    ): String {
        val parts = mutableListOf<String>()
        parts.add("Classification: ${classification.name}")
        parts.add("Time of day: ${state.timeOfDay.name}")

        // World Layer
        if (state.placeType != null) parts.add("Place type: ${state.placeType}")
        if (state.zoneCharacter != null) parts.add("Zone character: ${state.zoneCharacter}")
        if (state.tonalPalette != null) parts.add("Tonal palette: ${state.tonalPalette}")
        if (state.narrativeFunction != null) parts.add("Narrative function: ${state.narrativeFunction}")
        if (state.nearbyLandmarks.isNotEmpty()) {
            parts.add("Nearby landmarks: ${state.nearbyLandmarks.joinToString(", ")}")
        }

        // Action Layer
        parts.add("Movement: ${state.movementIntensity.name}")
        parts.add("Speed: ${"%.1f".format(state.speedKmh)} km/h")

        // Context
        if (weather != null) parts.add("Weather: $weather")
        if (state.minutesInCurrentScene > 0) {
            parts.add("Minutes in current scene: ${state.minutesInCurrentScene}")
        }

        state.previousClassification?.let {
            parts.add("Previous scene: ${it.name}")
            parts.add("Transition: ${it.name} -> ${classification.name}")
        }

        return parts.joinToString("\n")
    }
}
