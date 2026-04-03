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

    suspend fun analyzeLibrary(
        maxTracks: Int = 200,
        onProgress: (analyzed: Int, total: Int) -> Unit = { _, _ -> }
    ): Int {
        // Check if we already have a tagged library
        val existingCount = db.taggedSongDao().count()
        if (existingCount > 50) {
            Log.d(TAG, "Library already analyzed ($existingCount songs). Skipping.")
            return existingCount
        }

        Log.d(TAG, "Fetching saved tracks from Spotify...")
        val tracks = spotifyApi.getAllSavedTracks(maxTracks)
        if (tracks.isEmpty()) {
            Log.w(TAG, "No saved tracks found")
            return 0
        }
        Log.d(TAG, "Found ${tracks.size} tracks")

        // Fetch audio features
        val trackIds = tracks.map { it.id }
        val audioFeatures = spotifyApi.getAudioFeatures(trackIds)
        val featuresMap = audioFeatures.associateBy { it.id }

        // Batch-tag via Gemini
        var analyzed = 0
        tracks.chunked(BATCH_SIZE).forEach { batch ->
            val taggedBatch = tagBatch(batch, featuresMap)
            if (taggedBatch.isNotEmpty()) {
                db.taggedSongDao().insertAll(taggedBatch)
                analyzed += taggedBatch.size
                onProgress(analyzed, tracks.size)
            }
            // Rate limit: don't hammer Gemini
            delay(1000)
        }

        Log.d(TAG, "Library analysis complete: $analyzed songs tagged")
        return analyzed
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
        ) ?: return fallbackTag(tracks, featuresMap)

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
