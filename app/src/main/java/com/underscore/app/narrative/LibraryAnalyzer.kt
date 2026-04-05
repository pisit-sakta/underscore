package com.underscore.app.narrative

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.underscore.app.api.AudioFeatures
import com.underscore.app.api.LlmProvider
import com.underscore.app.api.SpotifyTrack
import com.underscore.app.api.SpotifyWebApi
import com.underscore.app.data.SongDatabase
import com.underscore.app.data.TaggedSong
import kotlinx.coroutines.delay

data class GeminiTagResult(
    val spotify_uri: String,
    val scene_types: List<String>,
    val energy_curve: String,
    val emotional_register: List<String>,
    val best_for: String,
    val avoid_for: String,
    val cultural_context: String
)

class LibraryAnalyzer(
    private val spotifyApi: SpotifyWebApi,
    private val geminiApi: LlmProvider,
    private val db: SongDatabase
) {
    companion object {
        private const val TAG = "LibraryAnalyzer"
        private const val BATCH_SIZE = 10 // Songs per Gemini call
    }

    private val gson = Gson()

    data class AnalysisResult(
        val taggedCount: Int,
        val spotifyFetchedCount: Int,
        val apiError: String? = null
    )

    suspend fun analyzeLibrary(
        onProgress: (analyzed: Int, total: Int) -> Unit = { _, _ -> },
        onFetchProgress: (fetchedSoFar: Int, estimatedTotal: Int) -> Unit = { _, _ -> }
    ): AnalysisResult {
        // Check how many songs we already have tagged
        val existingCount = db.taggedSongDao().count()

        Log.d(TAG, "Fetching all user tracks from Spotify (existing tags: $existingCount)...")
        val tracks = spotifyApi.getAllUserTracks(onProgress = onFetchProgress)
        if (tracks.isEmpty()) {
            Log.w(TAG, "No tracks found from Spotify")
            return AnalysisResult(existingCount, 0, apiError = spotifyApi.lastApiError)
        }
        Log.d(TAG, "Found ${tracks.size} unique tracks from Spotify")

        // Skip tracks that are already tagged
        val existingUris = db.taggedSongDao().getAllUris().toSet()
        val newTracks = tracks.filter { it.uri !in existingUris }
        if (newTracks.isEmpty()) {
            Log.d(TAG, "All ${tracks.size} tracks already tagged. Skipping.")
            return AnalysisResult(existingCount, tracks.size)
        }
        Log.d(TAG, "${newTracks.size} new tracks to analyze (${existingUris.size} already tagged)")

        // Fetch audio features for new tracks only
        val trackIds = newTracks.map { it.id }
        val audioFeatures = spotifyApi.getAudioFeatures(trackIds)
        val featuresMap = audioFeatures.associateBy { it.id }

        // Batch-tag via LLM (falls back to audio-feature tags if LLM unavailable)
        var analyzed = 0
        val totalNew = newTracks.size
        newTracks.chunked(BATCH_SIZE).forEach { batch ->
            val taggedBatch = tagBatch(batch, featuresMap)
            if (taggedBatch.isNotEmpty()) {
                db.taggedSongDao().insertAll(taggedBatch)
                analyzed += taggedBatch.size
                onProgress(existingCount + analyzed, existingCount + totalNew)
            }
            // Rate limit: don't hammer the LLM
            delay(1000)
        }

        val totalCount = existingCount + analyzed
        Log.d(TAG, "Library analysis complete: $analyzed new songs tagged ($totalCount total)")
        return AnalysisResult(totalCount, tracks.size)
    }

    private suspend fun tagBatch(
        tracks: List<SpotifyTrack>,
        featuresMap: Map<String, AudioFeatures>
    ): List<TaggedSong> {
        val tracksForTagging = tracks.map { track ->
            val features = featuresMap[track.id]
            TrackForTagging(
                uri = track.uri,
                title = track.name.replace("\"", "'"),
                artist = track.artistName.replace("\"", "'"),
                energy = features?.energy ?: 0.5f,
                valence = features?.valence ?: 0.5f,
                tempo = features?.tempo ?: 120f
            )
        }

        val prompt = Prompts.buildTaggingPrompt(tracksForTagging)
        val response = geminiApi.generate(
            prompt = prompt,
            systemPrompt = Prompts.LIBRARY_TAGGER,
            temperature = 0.4f,
            maxTokens = 4096,
            jsonMode = true
        )
        if (response == null) {
            Log.w(TAG, "LLM returned null for batch of ${tracks.size} tracks — using audio-feature fallback tags")
            return fallbackTag(tracks, featuresMap)
        }

        return try {
            val type = object : TypeToken<List<GeminiTagResult>>() {}.type
            val results: List<GeminiTagResult> = gson.fromJson(response, type)

            results.mapNotNull { result ->
                val track = tracks.find { it.uri == result.spotify_uri } ?: return@mapNotNull null
                val features = featuresMap[track.id]

                TaggedSong(
                    spotifyUri = track.uri,
                    title = track.name,
                    artist = track.artistName,
                    album = track.albumName,
                    sceneTypes = gson.toJson(result.scene_types),
                    energyCurve = result.energy_curve,
                    emotionalRegister = gson.toJson(result.emotional_register),
                    bestFor = result.best_for,
                    avoidFor = result.avoid_for,
                    culturalContext = result.cultural_context,
                    energy = features?.energy ?: 0.5f,
                    valence = features?.valence ?: 0.5f,
                    tempo = features?.tempo ?: 120f
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Gemini response", e)
            fallbackTag(tracks, featuresMap)
        }
    }

    private fun fallbackTag(
        tracks: List<SpotifyTrack>,
        featuresMap: Map<String, AudioFeatures>
    ): List<TaggedSong> {
        // If Gemini fails, create basic tags from audio features
        return tracks.map { track ->
            val features = featuresMap[track.id]
            val energy = features?.energy ?: 0.5f
            val valence = features?.valence ?: 0.5f

            val sceneTypes = when {
                energy > 0.8f -> listOf("active", "confrontation", "transit_fast")
                energy > 0.5f -> listOf("transit", "walking", "daytime")
                valence > 0.6f -> listOf("morning", "social", "walking")
                else -> listOf("evening", "night", "contemplative")
            }

            TaggedSong(
                spotifyUri = track.uri,
                title = track.name,
                artist = track.artistName,
                album = track.albumName,
                sceneTypes = gson.toJson(sceneTypes),
                energyCurve = if (energy > 0.6f) "sustained_high" else "ambient",
                emotionalRegister = gson.toJson(listOf(
                    if (valence > 0.5f) "upbeat" else "melancholic",
                    if (energy > 0.5f) "energetic" else "calm"
                )),
                bestFor = "Auto-tagged from audio features",
                avoidFor = "",
                culturalContext = "",
                energy = energy,
                valence = valence,
                tempo = features?.tempo ?: 120f
            )
        }
    }
}
