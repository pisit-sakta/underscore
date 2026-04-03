package com.underscore.app.playback

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NowPlaying(
    val trackName: String = "",
    val artistName: String = "",
    val trackUri: String = "",
    val isPaused: Boolean = true
)

/**
 * Controls Spotify playback via the App Remote SDK.
 *
 * The Spotify App Remote AAR must be in app/libs/ for this to work at runtime.
 * The code uses reflection to avoid a hard compile-time dependency, so the project
 * builds even without the AAR present. On first run, if the AAR is missing,
 * connect() will log an error and isConnected stays false.
 */
class PlaybackController(private val context: Context) {

    companion object {
        private const val TAG = "PlaybackController"
    }

    private var spotifyRemote: Any? = null // SpotifyAppRemote instance via reflection

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _nowPlaying = MutableStateFlow(NowPlaying())
    val nowPlaying: StateFlow<NowPlaying> = _nowPlaying.asStateFlow()

    fun connect() {
        Log.d(TAG, "Connecting to Spotify App Remote...")
        try {
            // Use reflection so the project compiles without the AAR
            val paramsClass = Class.forName("com.spotify.android.appremote.api.ConnectionParams")
            val builderClass = Class.forName("com.spotify.android.appremote.api.ConnectionParams\$Builder")
            val remoteClass = Class.forName("com.spotify.android.appremote.api.SpotifyAppRemote")
            val connectorClass = Class.forName("com.spotify.android.appremote.api.Connector")
            val listenerClass = Class.forName("com.spotify.android.appremote.api.Connector\$ConnectionListener")

            val builder = builderClass.getConstructor(String::class.java)
                .newInstance(com.underscore.app.auth.SpotifyAuth.CLIENT_ID)

            val setRedirectUri = builderClass.getMethod("setRedirectUri", String::class.java)
            setRedirectUri.invoke(builder, com.underscore.app.auth.SpotifyAuth.REDIRECT_URI)

            val showAuthView = builderClass.getMethod("showAuthView", Boolean::class.java)
            showAuthView.invoke(builder, false)

            val buildMethod = builderClass.getMethod("build")
            val params = buildMethod.invoke(builder)

            // Create a ConnectionListener via dynamic proxy
            val listener = java.lang.reflect.Proxy.newProxyInstance(
                listenerClass.classLoader,
                arrayOf(listenerClass)
            ) { _, method, args ->
                when (method.name) {
                    "onConnected" -> {
                        spotifyRemote = args?.get(0)
                        _isConnected.value = true
                        Log.d(TAG, "Connected to Spotify App Remote")
                        subscribeToPlayerState()
                    }
                    "onFailure" -> {
                        _isConnected.value = false
                        Log.e(TAG, "Failed to connect: ${args?.get(0)}")
                    }
                }
                null
            }

            val connectMethod = remoteClass.getMethod("connect", Context::class.java, paramsClass, connectorClass)
            connectMethod.invoke(null, context, params, listener)

        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "Spotify App Remote SDK not found. Place the AAR in app/libs/")
            _isConnected.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Spotify", e)
            _isConnected.value = false
        }
    }

    fun disconnect() {
        try {
            if (spotifyRemote != null) {
                val remoteClass = Class.forName("com.spotify.android.appremote.api.SpotifyAppRemote")
                val disconnectMethod = remoteClass.getMethod("disconnect", remoteClass)
                disconnectMethod.invoke(null, spotifyRemote)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting", e)
        }
        spotifyRemote = null
        _isConnected.value = false
    }

    fun playTrack(spotifyUri: String) {
        val remote = spotifyRemote ?: run {
            Log.w(TAG, "Not connected — can't play $spotifyUri")
            // Update UI even without connection for testing
            _nowPlaying.value = NowPlaying(
                trackName = spotifyUri.substringAfterLast(":"),
                artistName = "(not connected)",
                trackUri = spotifyUri,
                isPaused = false
            )
            return
        }

        try {
            Log.d(TAG, "Playing: $spotifyUri")
            val getPlayerApi = remote.javaClass.getMethod("getPlayerApi")
            val playerApi = getPlayerApi.invoke(remote)
            val playMethod = playerApi.javaClass.getMethod("play", String::class.java)
            playMethod.invoke(playerApi, spotifyUri)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing track", e)
        }
    }

    fun pause() {
        val remote = spotifyRemote ?: return
        try {
            val getPlayerApi = remote.javaClass.getMethod("getPlayerApi")
            val playerApi = getPlayerApi.invoke(remote)
            val pauseMethod = playerApi.javaClass.getMethod("pause")
            pauseMethod.invoke(playerApi)
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing", e)
        }
    }

    fun resume() {
        val remote = spotifyRemote ?: return
        try {
            val getPlayerApi = remote.javaClass.getMethod("getPlayerApi")
            val playerApi = getPlayerApi.invoke(remote)
            val resumeMethod = playerApi.javaClass.getMethod("resume")
            resumeMethod.invoke(playerApi)
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming", e)
        }
    }

    private fun subscribeToPlayerState() {
        val remote = spotifyRemote ?: return
        try {
            val getPlayerApi = remote.javaClass.getMethod("getPlayerApi")
            val playerApi = getPlayerApi.invoke(remote)
            val subscribeMethod = playerApi.javaClass.getMethod("subscribeToPlayerState")
            val subscription = subscribeMethod.invoke(playerApi)

            // Use reflection to set event callback
            val callbackClass = Class.forName("com.spotify.protocol.client.Subscription\$EventCallback")
            val callback = java.lang.reflect.Proxy.newProxyInstance(
                callbackClass.classLoader,
                arrayOf(callbackClass)
            ) { _, _, args ->
                try {
                    val state = args?.get(0) ?: return@newProxyInstance null
                    val track = state.javaClass.getField("track").get(state)
                    val isPaused = state.javaClass.getField("isPaused").getBoolean(state)

                    if (track != null) {
                        val name = track.javaClass.getField("name").get(track) as? String ?: ""
                        val artist = track.javaClass.getField("artist").get(track)
                        val artistName = artist?.javaClass?.getField("name")?.get(artist) as? String ?: ""
                        val uri = track.javaClass.getField("uri").get(track) as? String ?: ""

                        _nowPlaying.value = NowPlaying(
                            trackName = name,
                            artistName = artistName,
                            trackUri = uri,
                            isPaused = isPaused
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading player state", e)
                }
                null
            }

            val setEventCallback = subscription.javaClass.getMethod("setEventCallback", callbackClass)
            setEventCallback.invoke(subscription, callback)

        } catch (e: Exception) {
            Log.e(TAG, "Error subscribing to player state", e)
        }
    }
}
