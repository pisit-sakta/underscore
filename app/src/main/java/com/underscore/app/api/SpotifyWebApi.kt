package com.underscore.app.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    /** Last HTTP error encountered, for surfacing to UI */
    var lastApiError: String? = null
        private set

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
            delay(100)
        }

        Log.d(TAG, "getAllSavedTracks complete: ${tracks.size} tracks")
        return tracks
    }

    // ── Top Tracks (what the user actually listens to) ──

    data class TopTracksResponse(
        val items: List<SpotifyTrack>,
        val total: Int,
        val next: String?
    )

    suspend fun getTopTracks(timeRange: String = "medium_term", limit: Int = 50, offset: Int = 0): TopTracksResponse? {
        return get("$baseUrl/me/top/tracks?time_range=$timeRange&limit=$limit&offset=$offset", TopTracksResponse::class.java)
    }

    // ── Recently Played ──

    data class PlayHistoryItem(
        val track: SpotifyTrack
    )

    data class RecentlyPlayedResponse(
        val items: List<PlayHistoryItem>
    )

    suspend fun getRecentlyPlayed(limit: Int = 50): RecentlyPlayedResponse? {
        return get("$baseUrl/me/player/recently-played?limit=$limit", RecentlyPlayedResponse::class.java)
    }

    // ── Playlists ──

    data class PlaylistItem(
        val id: String,
        val name: String,
        val tracks: PlaylistTracksRef
    )

    data class PlaylistTracksRef(val total: Int)

    data class PlaylistsResponse(
        val items: List<PlaylistItem>,
        val next: String?
    )

    data class PlaylistTracksResponse(
        val items: List<PlaylistTrackItem>,
        val next: String?
    )

    data class PlaylistTrackItem(
        val track: SpotifyTrack?
    )

    suspend fun getMyPlaylists(limit: Int = 50, offset: Int = 0): PlaylistsResponse? {
        return get("$baseUrl/me/playlists?limit=$limit&offset=$offset", PlaylistsResponse::class.java)
    }

    suspend fun getPlaylistTracks(playlistId: String, limit: Int = 50, offset: Int = 0): PlaylistTracksResponse? {
        return get("$baseUrl/playlists/$playlistId/tracks?limit=$limit&offset=$offset&fields=items(track(id,name,artists,album,duration_ms,uri)),next", PlaylistTracksResponse::class.java)
    }

    /**
     * Fetch ALL tracks the user actually listens to:
     * top tracks (short/medium/long term) + recently played + liked songs + playlists.
     * Deduplicates by track ID.
     */
    suspend fun getAllUserTracks(
        onProgress: (fetchedSoFar: Int) -> Unit = {}
    ): List<SpotifyTrack> {
        val seen = mutableSetOf<String>()
        val tracks = mutableListOf<SpotifyTrack>()

        fun addUnique(newTracks: List<SpotifyTrack>) {
            for (t in newTracks) {
                if (t.id !in seen) {
                    seen.add(t.id)
                    tracks.add(t)
                }
            }
            onProgress(tracks.size)
        }

        // 1. Top tracks across all time ranges
        for (range in listOf("short_term", "medium_term", "long_term")) {
            var offset = 0
            while (true) {
                val response = getTopTracks(range, 50, offset) ?: break
                addUnique(response.items)
                if (response.next == null) break
                offset += 50
                delay(100)
            }
        }
        Log.d(TAG, "After top tracks: ${tracks.size} unique")

        // 2. Recently played
        val recent = getRecentlyPlayed(50)
        if (recent != null) {
            addUnique(recent.items.map { it.track })
        }
        Log.d(TAG, "After recently played: ${tracks.size} unique")

        // 3. Liked songs
        val saved = getAllSavedTracks()
        addUnique(saved)
        Log.d(TAG, "After liked songs: ${tracks.size} unique")

        // 4. User's playlists — ALL playlists, ALL tracks
        val allPlaylists = mutableListOf<PlaylistItem>()
        var plOffset = 0
        while (true) {
            val plResponse = getMyPlaylists(50, plOffset) ?: break
            allPlaylists.addAll(plResponse.items)
            if (plResponse.next == null) break
            plOffset += 50
            delay(100)
        }
        Log.d(TAG, "Found ${allPlaylists.size} playlists")

        allPlaylists.forEach { playlist ->
            var offset = 0
            while (true) {
                val ptResponse = getPlaylistTracks(playlist.id, 50, offset) ?: break
                addUnique(ptResponse.items.mapNotNull { it.track })
                if (ptResponse.next == null) break
                offset += 50
                delay(100)
            }
        }
        Log.d(TAG, "After playlists: ${tracks.size} unique")

        Log.d(TAG, "getAllUserTracks complete: ${tracks.size} unique tracks")
        return tracks
    }

    // ── Search (for character mode — franchise soundtracks) ──

    data class SearchTracksResult(
        val tracks: SearchTracksWrapper?
    )

    data class SearchTracksWrapper(
        val items: List<SpotifyTrack>
    )

    suspend fun searchTracks(query: String, limit: Int = 10): List<SpotifyTrack> {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val response = get(
            "$baseUrl/search?q=$encoded&type=track&limit=$limit",
            SearchTracksResult::class.java
        )
        return response?.tracks?.items ?: emptyList()
    }

    suspend fun getAudioFeatures(trackIds: List<String>): List<AudioFeatures> {
        if (trackIds.isEmpty()) return emptyList()

        val allFeatures = mutableListOf<AudioFeatures>()
        trackIds.chunked(100).forEachIndexed { index, chunk ->
            val ids = chunk.joinToString(",")
            val response = get(
                "$baseUrl/audio-features?ids=$ids",
                AudioFeaturesResponse::class.java
            )
            if (response == null) {
                Log.w(TAG, "getAudioFeatures failed for chunk of ${chunk.size} tracks")
            }
            response?.audioFeatures?.filterNotNull()?.let { allFeatures.addAll(it) }
            if (index < trackIds.chunked(100).size - 1) delay(100)
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

            var response = client.newCall(request).execute()

            // Handle rate limiting — wait and retry once
            if (response.code == 429) {
                val retryAfter = response.header("Retry-After")?.toLongOrNull() ?: 2
                Log.w(TAG, "Rate limited, waiting ${retryAfter}s before retry: ${url.substringBefore("?")}")
                response.close()
                delay(retryAfter * 1000)
                response = client.newCall(request).execute()
            }

            if (!response.isSuccessful) {
                val errorBody = response.body?.string()?.take(300)
                Log.e(TAG, "Spotify API HTTP ${response.code} for ${url.substringBefore("?")}: $errorBody")
                if (lastApiError == null) lastApiError = "HTTP ${response.code}"
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
