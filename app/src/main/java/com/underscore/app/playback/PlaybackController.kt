package com.underscore.app.playback

import android.content.Context
import android.util.Log
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.underscore.app.auth.SpotifyAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NowPlaying(
    val trackName: String = "",
    val artistName: String = "",
    val trackUri: String = "",
    val isPaused: Boolean = true
)

class PlaybackController(private val context: Context) {

    companion object {
        private const val TAG = "PlaybackController"
    }

    private var spotifyAppRemote: SpotifyAppRemote? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _nowPlaying = MutableStateFlow(NowPlaying())
    val nowPlaying: StateFlow<NowPlaying> = _nowPlaying.asStateFlow()

    private val connectionParams = ConnectionParams.Builder(SpotifyAuth.CLIENT_ID)
        .setRedirectUri(SpotifyAuth.REDIRECT_URI)
        .showAuthView(false)
        .build()

    fun connect() {
        Log.d(TAG, "Connecting to Spotify App Remote...")
        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(remote: SpotifyAppRemote) {
                spotifyAppRemote = remote
                _isConnected.value = true
                Log.d(TAG, "Connected to Spotify App Remote")
                subscribeToPlayerState()
            }

            override fun onFailure(error: Throwable) {
                _isConnected.value = false
                Log.e(TAG, "Failed to connect to Spotify App Remote", error)
            }
        })
    }

    fun disconnect() {
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }
        spotifyAppRemote = null
        _isConnected.value = false
    }

    fun playTrack(spotifyUri: String) {
        val remote = spotifyAppRemote ?: run {
            Log.w(TAG, "Not connected — can't play $spotifyUri")
            return
        }
        Log.d(TAG, "Playing: $spotifyUri")
        remote.playerApi.play(spotifyUri)
    }

    fun pause() {
        spotifyAppRemote?.playerApi?.pause()
    }

    fun resume() {
        spotifyAppRemote?.playerApi?.resume()
    }

    private fun subscribeToPlayerState() {
        spotifyAppRemote?.playerApi?.subscribeToPlayerState()?.setEventCallback { state ->
            val track = state.track
            _nowPlaying.value = NowPlaying(
                trackName = track?.name ?: "",
                artistName = track?.artist?.name ?: "",
                trackUri = track?.uri ?: "",
                isPaused = state.isPaused
            )
        }
    }
}
