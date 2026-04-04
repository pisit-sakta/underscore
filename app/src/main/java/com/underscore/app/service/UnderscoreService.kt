package com.underscore.app.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.underscore.app.debug.AppLog
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.underscore.app.MainActivity
import com.underscore.app.R
import com.underscore.app.UnderscoreApp
import com.underscore.app.api.ClaudeApi
import com.underscore.app.api.GeminiApi
import com.underscore.app.api.LlmProvider
import com.underscore.app.api.LlmProviderType
import com.underscore.app.api.OpenAiCompatibleApi
import com.underscore.app.api.SpotifyWebApi
import com.underscore.app.auth.SpotifyAuth
import com.underscore.app.context.ClassifiedScene
import com.underscore.app.context.ContextEngine
import com.underscore.app.context.ContextShift
import com.underscore.app.context.SceneClassification
import com.underscore.app.context.SceneState
import com.underscore.app.data.KnownLocationManager
import com.underscore.app.data.SceneHistoryEntry
import com.underscore.app.data.SongDatabase
import com.underscore.app.data.UserPreferences
import com.underscore.app.narrative.LibraryAnalyzer
import com.underscore.app.narrative.NarrativeEngine
import com.underscore.app.narrative.ProtagonistProfileManager
import com.underscore.app.playback.PlaybackController
import com.underscore.app.playback.TransitionManager
import com.underscore.app.sensor.HeartRateProvider
import com.underscore.app.sensor.PlacesProvider
import com.underscore.app.sensor.SensorAggregator
import com.underscore.app.sensor.WeatherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

class UnderscoreService : LifecycleService() {

    companion object {
        private const val TAG = "UnderscoreService"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "com.underscore.app.STOP_SCORING"
        private const val ACTION_SKIP = "com.underscore.app.SKIP_SONG"

        private val _currentScene = MutableStateFlow(SceneClassification.UNKNOWN)
        val currentScene: StateFlow<SceneClassification> = _currentScene.asStateFlow()

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _matchReason = MutableStateFlow("")
        val matchReason: StateFlow<String> = _matchReason.asStateFlow()

        private val _libraryStatus = MutableStateFlow("Not analyzed")
        val libraryStatus: StateFlow<String> = _libraryStatus.asStateFlow()

        private val _placeInfo = MutableStateFlow("")
        val placeInfo: StateFlow<String> = _placeInfo.asStateFlow()

        private val _heartRate = MutableStateFlow(0)
        val heartRate: StateFlow<Int> = _heartRate.asStateFlow()

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
    private lateinit var heartRateProvider: HeartRateProvider
    private lateinit var knownLocationManager: KnownLocationManager
    private lateinit var profileManager: ProtagonistProfileManager
    private lateinit var userPrefs: UserPreferences
    private lateinit var db: SongDatabase

    private var lastClassification: SceneClassification? = null
    private var currentSongUri: String? = null
    private var currentHistoryId: Long? = null

    override fun onCreate() {
        super.onCreate()
        AppLog.d(TAG, "Service created")

        userPrefs = UserPreferences(this)
        db = SongDatabase.getInstance(this)
        profileManager = ProtagonistProfileManager(this)
        knownLocationManager = KnownLocationManager(db)

        // Initialize sensor providers
        val placesKey = userPrefs.placesApiKey.ifEmpty { PlacesProvider.DEFAULT_API_KEY }
        val placesProvider = PlacesProvider(placesKey)
        sensorAggregator = SensorAggregator(this, placesProvider)
        heartRateProvider = HeartRateProvider(this)
        contextEngine = ContextEngine()
        playbackController = PlaybackController(this)
        transitionManager = TransitionManager(playbackController)

        val weatherKey = userPrefs.weatherApiKey.ifEmpty { WeatherProvider.DEFAULT_API_KEY }
        weatherProvider = WeatherProvider(weatherKey)

        // Initialize LLM provider based on user preference
        val llmProvider = createLlmProvider()
        narrativeEngine = NarrativeEngine(llmProvider, db, profileManager)
    }

    private fun createLlmProvider(): LlmProvider {
        return when (userPrefs.llmProvider) {
            LlmProviderType.GEMINI -> {
                val key = userPrefs.geminiApiKey.ifEmpty { GeminiApi.DEFAULT_API_KEY }
                GeminiApi(key)
            }
            LlmProviderType.CLAUDE -> {
                val key = userPrefs.claudeApiKey.ifEmpty { ClaudeApi.DEFAULT_API_KEY }
                ClaudeApi(key)
            }
            LlmProviderType.OPENAI_COMPATIBLE -> {
                val url = userPrefs.customApiUrl
                val model = userPrefs.customModel
                val key = userPrefs.customApiKey
                if (url.isNotBlank() && model.isNotBlank()) {
                    OpenAiCompatibleApi(baseUrl = url, model = model, apiKey = key)
                } else {
                    // Fallback to Gemini if custom not configured
                    val geminiKey = userPrefs.geminiApiKey.ifEmpty { GeminiApi.DEFAULT_API_KEY }
                    GeminiApi(geminiKey)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_STOP -> {
                stopScoring()
                return START_NOT_STICKY
            }
            ACTION_SKIP -> {
                handleSkip()
                return START_STICKY
            }
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
            try {
                analyzeLibraryIfNeeded()
            } catch (e: Exception) {
                AppLog.e(TAG, "Library analysis crashed: ${e.message}", e)
                _libraryStatus.value = "Analysis failed: ${e.message}"
            }
        }

        // Heart rate monitoring (if available)
        lifecycleScope.launch {
            try {
                heartRateProvider.heartRateUpdates().collect { hr ->
                    _heartRate.value = hr.bpm
                }
            } catch (e: Exception) {
                AppLog.e(TAG, "Heart rate flow failed: ${e.message}", e)
            }
        }

        // Main scoring pipeline
        lifecycleScope.launch {
            try {
            val sceneStates = sensorAggregator.sceneStateFlow()
            val classifiedScenes = contextEngine.classify(sceneStates)

            classifiedScenes
                .distinctUntilChangedBy { it.classification }
                .collectLatest { scene ->
                    val classification = scene.classification
                    var state = scene.sceneState

                    AppLog.d(TAG, "Scene: $classification (${scene.minutesInScene}min)")
                    _currentScene.value = classification

                    // Inject heart rate into scene state
                    val hr = heartRateProvider.getLastBpm()
                    val hrState = heartRateProvider.getLastState()
                    if (hr > 0) {
                        state = state.copy(
                            heartRateBpm = hr,
                            heartRateState = hrState.name.lowercase()
                        )
                    }

                    // Update place info for UI
                    val placeDesc = buildPlaceDescription(state)
                    if (placeDesc.isNotEmpty()) _placeInfo.value = placeDesc

                    // Check known location
                    val knownLocation = if (state.latitude != 0.0) {
                        knownLocationManager.checkLocation(
                            state.latitude, state.longitude, state.placeType
                        )
                    } else null

                    // Fetch weather
                    val weather = if (state.latitude != 0.0) {
                        weatherProvider.getWeather(state.latitude, state.longitude)
                    } else null

                    state = state.copy(weather = weather?.condition)

                    // Select song
                    val selection = narrativeEngine.selectSong(
                        sceneState = state,
                        classification = classification,
                        weather = weather?.let { "${it.condition} (${it.description}, ${it.temperatureC}°C)" },
                        knownLocation = knownLocation
                    )

                    AppLog.d(TAG, "Selected: ${selection.title} by ${selection.artist}")
                    _matchReason.value = selection.matchReason
                    currentSongUri = selection.spotifyUri

                    // Log to scene history
                    val historyEntry = SceneHistoryEntry(
                        classification = classification.name,
                        placeType = state.placeType,
                        zoneCharacter = state.zoneCharacter,
                        songUri = selection.spotifyUri,
                        songTitle = selection.title,
                        songArtist = selection.artist,
                        matchReason = selection.matchReason,
                        transitionType = selection.transitionType,
                        latitude = state.latitude,
                        longitude = state.longitude,
                        weather = state.weather,
                        minutesInScene = scene.minutesInScene
                    )
                    currentHistoryId = db.sceneHistoryDao().insert(historyEntry)

                    // Try to develop leitmotifs at known locations
                    knownLocation?.let {
                        knownLocationManager.tryDevelopLeitmotif(it.id)
                    }

                    // Play with transition
                    when (selection.transitionType) {
                        "dramatic_silence" -> transitionManager.dramaticSilence(selection.spotifyUri)
                        else -> {
                            val isUrgent = selection.transitionType == "urgent"
                            val shift = lastClassification?.let { prev ->
                                ContextShift(from = prev, to = classification, isUrgent = isUrgent)
                            }
                            transitionManager.transition(selection.spotifyUri, shift)
                        }
                    }

                    updateNotification(classification, selection.title, state.placeType)
                    lastClassification = classification
                }
            } catch (e: Exception) {
                AppLog.e(TAG, "SCORING PIPELINE CRASHED: ${e.message}", e)
                _matchReason.value = "Pipeline error: ${e.message}"
            }
        }
    }

    private fun handleSkip() {
        // Learning loop: mark current song as skipped
        val uri = currentSongUri ?: return
        val historyId = currentHistoryId
        lifecycleScope.launch {
            db.taggedSongDao().recordSkip(uri)
            if (historyId != null) {
                db.sceneHistoryDao().setFeedback(historyId, "skip")
            }
            AppLog.d(TAG, "Recorded skip for: $uri")
        }
    }

    fun recordBoost() {
        val uri = currentSongUri ?: return
        val historyId = currentHistoryId
        lifecycleScope.launch {
            db.taggedSongDao().recordBoost(uri)
            if (historyId != null) {
                db.sceneHistoryDao().setFeedback(historyId, "boost")
            }
            AppLog.d(TAG, "Recorded boost for: $uri")
        }
    }

    private fun buildPlaceDescription(state: SceneState): String {
        val parts = mutableListOf<String>()
        state.placeType?.let { if (it != "unknown") parts.add(it) }
        state.zoneCharacter?.let { if (it != "urban") parts.add("($it)") }
        return parts.joinToString(" ")
    }

    private suspend fun analyzeLibraryIfNeeded() {
        val spotifyAuth = SpotifyAuth(this)
        val token = spotifyAuth.getAccessToken()
        if (token == null) {
            AppLog.w(TAG, "No Spotify access token — library analysis skipped (user not logged in or token expired)")
            _libraryStatus.value = "No Spotify token"
            return
        }

        _libraryStatus.value = "Checking library..."

        val spotifyApi = SpotifyWebApi(token)
        val llmProvider = createLlmProvider()
        val analyzer = LibraryAnalyzer(spotifyApi, llmProvider, db)

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

    private fun updateNotification(
        scene: SceneClassification,
        trackTitle: String,
        placeType: String? = null
    ) {
        val sceneName = scene.name.lowercase().replace("_", " ")
        val place = if (placeType != null && placeType != "unknown") " @ $placeType" else ""
        val notification = buildNotification("$sceneName$place — $trackTitle")
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }
}
