package com.underscore.app.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class SpotifyTrack(
    val id: String,
    val name: String,
    val artists: List<SpotifyArtist>,
    val album: SpotifyAlbum,
    @SerializedName("duration_ms") val durationMs: Long,
    val uri: String
) {
    val artistName: String get() = artists.firstOrNull()?.name ?: "Unknown"
    val albumName: String get() = album.name
}

data class SpotifyArtist(val id: String, val name: String)
data class SpotifyAlbum(val id: String, val name: String)

data class SavedTrackItem(
    val track: SpotifyTrack
)

data class SavedTracksResponse(
    val items: List<SavedTrackItem>,
    val total: Int,
    val next: String?
)

data class AudioFeatures(
    val id: String,
    val energy: Float,
    val valence: Float,
    val tempo: Float,
    val danceability: Float,
    val acousticness: Float,
    val instrumentalness: Float
)

data class AudioFeaturesResponse(
    @SerializedName("audio_features") val audioFeatures: List<AudioFeatures?>
)

class SpotifyWebApi(private val accessToken: String) {

    companion object {
        private const val TAG = "SpotifyWebApi"
    }

    private val client = OkHttpClient()
    private val gson = Gson()
    private val baseUrl = "https://api.spotify.com/v1"

    suspend fun getSavedTracks(limit: Int = 50, offset: Int = 0): SavedTracksResponse? {
        Log.d(TAG, "Fetching saved tracks: limit=$limit offset=$offset")
        return get("$baseUrl/me/tracks?limit=$limit&offset=$offset", SavedTracksResponse::class.java)
    }

    suspend fun getAllSavedTracks(): List<SpotifyTrack> {
        val tracks = mutableListOf<SpotifyTrack>()
        var offset = 0
        val limit = 50

        while (true) {
            val response = getSavedTracks(limit, offset)
            if (response == null) {
                Log.w(TAG, "getSavedTracks returned null at offset=$offset, stopping pagination")
                break
            }
            tracks.addAll(response.items.map { it.track })
            Log.d(TAG, "Fetched ${tracks.size}/${response.total} tracks")
            if (response.next == null) break
            offset += limit
        }

        Log.d(TAG, "getAllSavedTracks complete: ${tracks.size} tracks")
        return tracks
    }

    suspend fun getAudioFeatures(trackIds: List<String>): List<AudioFeatures> {
        if (trackIds.isEmpty()) return emptyList()

        val allFeatures = mutableListOf<AudioFeatures>()
        trackIds.chunked(100).forEach { chunk ->
            val ids = chunk.joinToString(",")
            val response = get(
                "$baseUrl/audio-features?ids=$ids",
                AudioFeaturesResponse::class.java
            )
            if (response == null) {
                Log.w(TAG, "getAudioFeatures failed for chunk of ${chunk.size} tracks")
            }
            response?.audioFeatures?.filterNotNull()?.let { allFeatures.addAll(it) }
        }

        Log.d(TAG, "Got audio features for ${allFeatures.size}/${trackIds.size} tracks")
        return allFeatures
    }

    private suspend fun <T> get(url: String, clazz: Class<T>): T? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()?.take(300)
                Log.e(TAG, "Spotify API ${response.code} for ${url.substringBefore("?")}: $errorBody")
                return@withContext null
            }

            val body = response.body?.string()
            if (body == null) {
                Log.e(TAG, "Spotify returned null body for ${url.substringBefore("?")}")
                return@withContext null
            }

            gson.fromJson(body, clazz)
        } catch (e: Exception) {
            Log.e(TAG, "Spotify request failed for ${url.substringBefore("?")}: ${e.message}", e)
            null
        }
    }
}
