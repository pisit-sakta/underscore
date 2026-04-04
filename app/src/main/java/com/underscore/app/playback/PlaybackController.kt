package com.underscore.app.playback

import android.content.Context
import com.underscore.app.auth.SpotifyAuth
import com.underscore.app.debug.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class NowPlaying(
    val trackName: String = "",
    val artistName: String = "",
    val trackUri: String = "",
    val isPaused: Boolean = true
)

/**
 * Controls Spotify playback via the Web API.
 *
 * Uses the existing OAuth token (with user-modify-playback-state scope)
 * to control playback on the user's active Spotify device.
 */
class PlaybackController(private val context: Context) {

    companion object {
        private const val TAG = "PlaybackController"
        private const val BASE_URL = "https://api.spotify.com/v1/me/player"
    }

    private val spotifyAuth = SpotifyAuth(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _nowPlaying = MutableStateFlow(NowPlaying())
    val nowPlaying: StateFlow<NowPlaying> = _nowPlaying.asStateFlow()

    fun connect() {
        val token = spotifyAuth.getAccessToken()
        if (token != null) {
            _isConnected.value = true
            AppLog.d(TAG, "Connected (Web API mode, token available)")
            // Poll current playback to update now playing
            scope.launch { pollPlaybackState(token) }
        } else {
            _isConnected.value = false
            AppLog.w(TAG, "No access token — not connected")
        }
    }

    fun disconnect() {
        _isConnected.value = false
        AppLog.d(TAG, "Disconnected")
    }

    fun updateNowPlaying(title: String, artist: String, uri: String) {
        _nowPlaying.value = NowPlaying(
            trackName = title,
            artistName = artist,
            trackUri = uri,
            isPaused = false
        )
    }

    private var refreshAttempted = false

    fun playTrack(spotifyUri: String) {
        scope.launch {
            val token = spotifyAuth.getAccessToken()
            if (token == null) {
                AppLog.w(TAG, "No token — can't play $spotifyUri")
                return@launch
            }

            try {
                val json = """{"uris":["$spotifyUri"]}"""
                val body = json.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$BASE_URL/play")
                    .addHeader("Authorization", "Bearer $token")
                    .put(body)
                    .build()

                val response = client.newCall(request).execute()

                when (response.code) {
                    204, 200 -> {
                        AppLog.d(TAG, "Playing: $spotifyUri")
                        refreshAttempted = false
                    }
                    404 -> {
                        // No active device — try to find one and transfer
                        AppLog.w(TAG, "No active device. Trying to find one...")
                        val deviceId = findActiveDevice(token)
                        if (deviceId != null) {
                            playOnDevice(token, spotifyUri, deviceId)
                        } else {
                            AppLog.e(TAG, "No Spotify devices found. Open Spotify first.")
                        }
                    }
                    401, 403 -> {
                        val errorBody = response.body?.string()?.take(300) ?: ""
                        if (refreshAttempted) {
                            AppLog.e(TAG, "Playback failed after refresh (${response.code}). " +
                                "Re-login required for playback scopes. $errorBody")
                            refreshAttempted = false
                        } else {
                            AppLog.w(TAG, "Token rejected (${response.code}), refreshing once...")
                            refreshAttempted = true
                            if (spotifyAuth.refreshAccessToken()) {
                                playTrack(spotifyUri)
                            } else {
                                AppLog.e(TAG, "Token refresh failed. Re-login required.")
                                refreshAttempted = false
                            }
                        }
                    }
                    else -> {
                        val errorBody = response.body?.string()?.take(300)
                        AppLog.e(TAG, "Play failed (${response.code}): $errorBody")
                    }
                }
            } catch (e: Exception) {
                AppLog.e(TAG, "Error playing track: ${e.message}", e)
            }
        }
    }

    fun pause() {
        scope.launch {
            val token = spotifyAuth.getAccessToken() ?: return@launch
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/pause")
                    .addHeader("Authorization", "Bearer $token")
                    .put("".toRequestBody(null))
                    .build()
                client.newCall(request).execute()
                AppLog.d(TAG, "Paused")
            } catch (e: Exception) {
                AppLog.e(TAG, "Error pausing: ${e.message}", e)
            }
        }
    }

    fun resume() {
        scope.launch {
            val token = spotifyAuth.getAccessToken() ?: return@launch
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/play")
                    .addHeader("Authorization", "Bearer $token")
                    .put("".toRequestBody(null))
                    .build()
                client.newCall(request).execute()
                AppLog.d(TAG, "Resumed")
            } catch (e: Exception) {
                AppLog.e(TAG, "Error resuming: ${e.message}", e)
            }
        }
    }

    private suspend fun findActiveDevice(token: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/devices")
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null

            // Parse devices JSON manually to avoid adding models
            val devicesMatch = Regex(""""id"\s*:\s*"([^"]+)"""").findAll(body)
            val deviceId = devicesMatch.firstOrNull()?.groupValues?.get(1)

            if (deviceId != null) {
                AppLog.d(TAG, "Found device: $deviceId")
            } else {
                AppLog.w(TAG, "No devices in response: ${body.take(200)}")
            }

            deviceId
        } catch (e: Exception) {
            AppLog.e(TAG, "Error finding devices: ${e.message}", e)
            null
        }
    }

    private suspend fun playOnDevice(token: String, spotifyUri: String, deviceId: String) =
        withContext(Dispatchers.IO) {
            try {
                val json = """{"uris":["$spotifyUri"]}"""
                val body = json.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$BASE_URL/play?device_id=$deviceId")
                    .addHeader("Authorization", "Bearer $token")
                    .put(body)
                    .build()

                val response = client.newCall(request).execute()
                if (response.code in listOf(200, 204)) {
                    AppLog.d(TAG, "Playing on device $deviceId: $spotifyUri")
                } else {
                    val errorBody = response.body?.string()?.take(300)
                    AppLog.e(TAG, "Play on device failed (${response.code}): $errorBody")
                }
            } catch (e: Exception) {
                AppLog.e(TAG, "Error playing on device: ${e.message}", e)
            }
        }

    private suspend fun pollPlaybackState(token: String) = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL")
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.code == 200) {
                val body = response.body?.string() ?: return@withContext
                // Extract track name and artist from the playback state
                val trackName = Regex(""""name"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1) ?: ""
                if (trackName.isNotEmpty()) {
                    AppLog.d(TAG, "Currently playing: $trackName")
                }
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "Error polling playback: ${e.message}", e)
        }
    }
}
