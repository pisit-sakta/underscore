package com.underscore.app.narrative

import com.google.gson.Gson
import com.underscore.app.debug.AppLog
import com.google.gson.reflect.TypeToken
import com.underscore.app.api.LlmProvider
import com.underscore.app.context.SceneClassification
import com.underscore.app.context.SceneState
import com.underscore.app.data.DramaScale
import com.underscore.app.data.KnownLocation
import com.underscore.app.data.SongDatabase
import com.underscore.app.data.TaggedSong

data class SongSelection(
    val spotifyUri: String,
    val title: String,
    val artist: String,
    val matchReason: String,
    val transitionType: String,
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
    private val llmProvider: LlmProvider,
    private val db: SongDatabase,
    private val profileManager: ProtagonistProfileManager? = null
) {
    companion object {
        private const val TAG = "NarrativeEngine"
        private const val CANDIDATE_POOL_SIZE = 20
        private const val RECENTLY_PLAYED_WINDOW_MS = 30 * 60 * 1000L
    }

    private val gson = Gson()
    private val fallbackSelector = SongSelector()

    suspend fun selectSong(
        sceneState: SceneState,
        classification: SceneClassification,
        weather: String? = null,
        knownLocation: KnownLocation? = null,
        dramaScale: Int = 5,
        customMood: String? = null,
        characterProfile: CharacterProfile? = null
    ): SongSelection {
        val allSongs = db.taggedSongDao().getAll()

        if (allSongs.size < 3) {
            AppLog.d(TAG, "Library not analyzed yet (${allSongs.size} songs), using fallback selector")
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

        // Check for leitmotif at known location
        if (knownLocation?.leitmotifUri != null) {
            val leitmotifSong = db.taggedSongDao().getByUri(knownLocation.leitmotifUri)
            if (leitmotifSong != null && classification in stationaryClassifications()) {
                AppLog.d(TAG, "Playing leitmotif for ${knownLocation.label}: ${leitmotifSong.title}")
                db.taggedSongDao().recordPlay(leitmotifSong.spotifyUri)
                return SongSelection(
                    spotifyUri = leitmotifSong.spotifyUri,
                    title = leitmotifSong.title,
                    artist = leitmotifSong.artist,
                    matchReason = "Leitmotif for ${knownLocation.label}",
                    transitionType = "normal",
                    transitionDurationMs = 3000
                )
            }
        }

        val recentUris = db.taggedSongDao().getRecentlyPlayedUris(
            System.currentTimeMillis() - RECENTLY_PLAYED_WINDOW_MS
        )

        val candidates = preselectCandidates(allSongs, classification, recentUris, dramaScale)

        if (candidates.isEmpty()) {
            AppLog.w(TAG, "No candidates after filtering, using random")
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

        val sceneDescription = buildSceneDescription(sceneState, classification, weather, knownLocation, dramaScale, customMood)

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

        // Build protagonist context — character profile takes priority if active
        val protagonistContext = if (characterProfile != null) {
            profileManager?.buildCharacterPromptContext(characterProfile) ?: ""
        } else {
            profileManager?.buildPromptContext() ?: ""
        }

        val prompt = Prompts.buildScoringPrompt(sceneDescription, trackSummaries, recentUris.take(5), protagonistContext)
        AppLog.d(TAG, "Querying LLM (${llmProvider.name}) with ${candidates.size} candidates for $classification")

        val response = llmProvider.generate(
            prompt = prompt,
            systemPrompt = Prompts.SCENE_SCORER,
            temperature = 0.6f,
            maxTokens = 512,
            jsonMode = true
        )

        if (response == null) {
            AppLog.w(TAG, "LLM returned null — falling back to local selection")
        }

        if (response != null) {
            try {
                val selection = gson.fromJson(response, GeminiSongSelection::class.java)
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
                AppLog.e(TAG, "Failed to parse LLM selection", e)
            }
        }

        val pick = candidates.random()
        db.taggedSongDao().recordPlay(pick.spotifyUri)
        val errorDetail = llmProvider.lastError
        val reason = when {
            !llmProvider.isConfigured -> "Local selection (no API key — set in Settings)"
            errorDetail != null -> "Local selection ($errorDetail)"
            else -> "Local selection (LLM returned empty)"
        }
        return SongSelection(
            spotifyUri = pick.spotifyUri,
            title = pick.title,
            artist = pick.artist,
            matchReason = reason,
            transitionType = "normal",
            transitionDurationMs = 3000
        )
    }

    private fun stationaryClassifications() = setOf(
        SceneClassification.MORNING_STATIONARY,
        SceneClassification.DAYTIME_STATIONARY,
        SceneClassification.EVENING_STATIONARY,
        SceneClassification.NIGHT_STATIONARY
    )

    private fun preselectCandidates(
        allSongs: List<TaggedSong>,
        classification: SceneClassification,
        recentUris: List<String>,
        dramaScale: Int = 5
    ): List<TaggedSong> {
        val sceneKeywords = classificationToKeywords(classification)
        // Drama scale affects energy thresholds:
        // Low drama (1-3) = prefer calmer tracks even in moderate scenes
        // High drama (8-10) = allow intense tracks even in calm scenes (ironic-dramatic)
        val dramaFactor = dramaScale / 10f  // 0.1 to 1.0

        val scored = allSongs
            .filter { it.spotifyUri !in recentUris }
            .map { song ->
                val songSceneTypes: List<String> = try {
                    gson.fromJson(song.sceneTypes, object : TypeToken<List<String>>() {}.type)
                } catch (e: Exception) { emptyList() }

                val matchScore = songSceneTypes.count { sceneType ->
                    sceneKeywords.any { keyword -> sceneType.contains(keyword, ignoreCase = true) }
                }

                val energyScore = when (classification) {
                    SceneClassification.ACTIVE -> if (song.energy > 0.7f) 2 else 0
                    SceneClassification.TRANSIT -> if (song.energy in 0.4f..0.8f) 1 else 0
                    SceneClassification.NIGHT_STATIONARY -> {
                        // High drama: don't penalize high-energy tracks as hard
                        if (dramaScale >= 8) 0
                        else if (song.energy < 0.4f) 2 else 0
                    }
                    SceneClassification.EVENING_STATIONARY -> {
                        if (dramaScale >= 8) 0
                        else if (song.energy < 0.5f) 1 else 0
                    }
                    SceneClassification.MORNING_STATIONARY -> if (song.valence > 0.3f) 1 else 0
                    else -> 0
                }

                // At high drama, give bonus to high-energy tracks in ANY scene
                val dramaBonus = if (dramaScale >= 8 && song.energy > 0.7f) 1
                    else if (dramaScale <= 3 && song.energy < 0.4f) 1
                    else 0

                // Learning loop: boost songs user liked, penalize skipped
                val feedbackScore = song.boostCount - song.skipCount

                song to (matchScore + energyScore + dramaBonus + feedbackScore)
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
        weather: String?,
        knownLocation: KnownLocation? = null,
        dramaScale: Int = 5,
        customMood: String? = null
    ): String {
        val parts = mutableListOf<String>()
        parts.add("Classification: ${classification.name}")
        parts.add("Time of day: ${state.timeOfDay.name}")

        // Drama Scale context for LLM
        val dramaLevel = DramaScale.getLevel(dramaScale)
        parts.add("Drama Scale: ${dramaScale}/10 (${dramaLevel.name})")
        parts.add("Drama instruction: ${dramaLevel.llmInstruction}")

        // Custom mood — the user's self-described emotional state
        if (!customMood.isNullOrBlank()) {
            parts.add("User's current mood/vibe: \"$customMood\"")
            parts.add("Mood instruction: The user has set their vibe to \"$customMood\". " +
                "Interpret this as an emotional/aesthetic context and let it COLOR your song selection. " +
                "This is a subjective feeling — use your cultural knowledge to understand what this means " +
                "and select songs that honor this vibe while still respecting the scene context.")
        }

        // World Layer
        if (state.placeType != null) parts.add("Place type: ${state.placeType}")
        if (state.zoneCharacter != null) parts.add("Zone character: ${state.zoneCharacter}")
        if (state.tonalPalette != null) parts.add("Tonal palette: ${state.tonalPalette}")
        if (state.narrativeFunction != null) parts.add("Narrative function: ${state.narrativeFunction}")
        if (state.nearbyLandmarks.isNotEmpty()) {
            parts.add("Nearby landmarks: ${state.nearbyLandmarks.joinToString(", ")}")
        }

        // Known location context
        if (knownLocation != null) {
            parts.add("Known location: ${knownLocation.label} (visited ${knownLocation.visitCount} times)")
            if (knownLocation.leitmotifTitle != null) {
                parts.add("Location leitmotif: ${knownLocation.leitmotifTitle}")
            }
        }

        // Action Layer
        parts.add("Movement: ${state.movementIntensity.name}")
        parts.add("Speed: ${"%.1f".format(state.speedKmh)} km/h")
        if (state.heartRateBpm > 0) {
            parts.add("Heart rate: ${state.heartRateBpm} bpm (${state.heartRateState})")
        }

        // Layer priority
        parts.add("Layer priority: ${deriveLayerPriority(state, classification)}")

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

    private fun deriveLayerPriority(state: SceneState, classification: SceneClassification): String {
        val actionIntensity = when (state.movementIntensity) {
            com.underscore.app.context.MovementIntensity.STILL -> 0.0f
            com.underscore.app.context.MovementIntensity.LIGHT -> 0.25f
            com.underscore.app.context.MovementIntensity.MODERATE -> 0.55f
            com.underscore.app.context.MovementIntensity.INTENSE -> 0.9f
        }

        // Heart rate can spike action priority even when still
        val hrBoost = when {
            state.heartRateBpm > 140 -> 0.3f
            state.heartRateBpm > 110 -> 0.15f
            else -> 0.0f
        }

        val effectiveIntensity = (actionIntensity + hrBoost).coerceAtMost(1.0f)

        return when {
            effectiveIntensity < 0.2f -> "world_dominant"
            effectiveIntensity < 0.4f -> "blended"
            effectiveIntensity < 0.7f -> "action_dominant_world_colored"
            else -> when {
                state.previousClassification in stationaryClassifications() ->
                    "action_dominant_post_departure"
                state.heartRateBpm > 120 && state.movementIntensity == com.underscore.app.context.MovementIntensity.STILL ->
                    "world_dominant_action_tension"
                else -> "action_dominant_world_colored"
            }
        }
    }
}
