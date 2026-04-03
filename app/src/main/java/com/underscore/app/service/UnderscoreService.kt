package com.underscore.app.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.underscore.app.MainActivity
import com.underscore.app.R
import com.underscore.app.UnderscoreApp
import com.underscore.app.api.GeminiApi
import com.underscore.app.api.SpotifyWebApi
import com.underscore.app.auth.SpotifyAuth
import com.underscore.app.context.ContextEngine
import com.underscore.app.context.SceneClassification
import com.underscore.app.context.SceneState
import com.underscore.app.data.SongDatabase
import com.underscore.app.narrative.LibraryAnalyzer
import com.underscore.app.narrative.NarrativeEngine
import com.underscore.app.playback.PlaybackController
import com.underscore.app.playback.TransitionManager
import com.underscore.app.sensor.SensorAggregator
import com.underscore.app.sensor.WeatherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UnderscoreService : LifecycleService() {

    companion object {
        private const val TAG = "UnderscoreService"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "com.underscore.app.STOP_SCORING"

        private val _currentScene = MutableStateFlow(SceneClassification.UNKNOWN)
        val currentScene: StateFlow<SceneClassification> = _currentScene.asStateFlow()

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _matchReason = MutableStateFlow("")
        val matchReason: StateFlow<String> = _matchReason.asStateFlow()

        private val _libraryStatus = MutableStateFlow("Not analyzed")
        val libraryStatus: StateFlow<String> = _libraryStatus.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, UnderscoreService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, UnderscoreService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private lateinit var sensorAggregator: SensorAggregator
    private lateinit var contextEngine: ContextEngine
    private lateinit var playbackController: PlaybackController
    private lateinit var transitionManager: TransitionManager
    private lateinit var narrativeEngine: NarrativeEngine
    private lateinit var weatherProvider: WeatherProvider
    private lateinit var db: SongDatabase

    private var lastClassification: SceneClassification? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        sensorAggregator = SensorAggregator(this)
        contextEngine = ContextEngine()
        playbackController = PlaybackController(this)
        transitionManager = TransitionManager(playbackController)
        weatherProvider = WeatherProvider(WeatherProvider.DEFAULT_API_KEY)
        db = SongDatabase.getInstance(this)

        val geminiApi = GeminiApi(GeminiApi.DEFAULT_API_KEY)
        narrativeEngine = NarrativeEngine(geminiApi, db)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_STOP) {
            stopScoring()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification("Starting..."))
        startScoring()

        return START_STICKY
    }

    private fun startScoring() {
        _isRunning.value = true
        playbackController.connect()

        // Analyze library in background if needed
        lifecycleScope.launch {
            analyzeLibraryIfNeeded()
        }

        // Main pipeline: sensors -> context -> narrative engine -> play
        lifecycleScope.launch {
            val sceneStates = sensorAggregator.sceneStateFlow()
            val classifications = contextEngine.classify(sceneStates)

            var latestState: SceneState? = null

            // Track latest scene state for weather/GPS data
            launch {
                sensorAggregator.sceneStateFlow().collectLatest { state ->
                    latestState = state
                }
            }

            classifications.collectLatest { classification ->
                Log.d(TAG, "Scene: $classification")
                _currentScene.value = classification

                val isNewScene = lastClassification == null || lastClassification != classification

                if (isNewScene) {
                    val state = latestState ?: SceneState()

                    // Fetch weather (cached, won't block if recent)
                    val weather = if (state.latitude != 0.0) {
                        weatherProvider.getWeather(state.latitude, state.longitude)
                    } else null

                    // Use narrative engine for song selection
                    val selection = narrativeEngine.selectSong(
                        sceneState = state.copy(
                            previousClassification = lastClassification,
                            weather = weather?.condition
                        ),
                        classification = classification,
                        weather = weather?.let { "${it.condition} (${it.description}, ${it.temperatureC}°C)" }
                    )

                    Log.d(TAG, "Selected: ${selection.title} by ${selection.artist}")
                    Log.d(TAG, "Reason: ${selection.matchReason}")
                    _matchReason.value = selection.matchReason

                    // Play with appropriate transition
                    when (selection.transitionType) {
                        "dramatic_silence" -> transitionManager.dramaticSilence(selection.spotifyUri)
                        else -> {
                            val isUrgent = selection.transitionType == "urgent"
                            val shift = lastClassification?.let { prev ->
                                com.underscore.app.context.ContextShift(
                                    from = prev,
                                    to = classification,
                                    isUrgent = isUrgent
                                )
                            }
                            transitionManager.transition(selection.spotifyUri, shift)
                        }
                    }

                    updateNotification(classification, selection.title)
                }

                lastClassification = classification
            }
        }
    }

    private suspend fun analyzeLibraryIfNeeded() {
        val spotifyAuth = SpotifyAuth(this)
        val token = spotifyAuth.getAccessToken() ?: return

        _libraryStatus.value = "Checking library..."

        val spotifyApi = SpotifyWebApi(token)
        val geminiApi = GeminiApi(GeminiApi.DEFAULT_API_KEY)
        val analyzer = LibraryAnalyzer(spotifyApi, geminiApi, db)

        val count = analyzer.analyzeLibrary(
            maxTracks = 200,
            onProgress = { analyzed, total ->
                _libraryStatus.value = "Analyzing: $analyzed / $total"
            }
        )

        _libraryStatus.value = "$count songs ready"
    }

    private fun stopScoring() {
        _isRunning.value = false
        playbackController.pause()
        playbackController.disconnect()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        playbackController.disconnect()
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, UnderscoreApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Underscore")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(scene: SceneClassification, trackTitle: String) {
        val sceneName = scene.name.lowercase().replace("_", " ")
        val notification = buildNotification("$sceneName — $trackTitle")
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }
}
