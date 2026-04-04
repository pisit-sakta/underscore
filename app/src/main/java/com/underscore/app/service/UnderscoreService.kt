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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
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

        // Sensor debug data for UI
        private val _latitude = MutableStateFlow(0.0)
        val latitude: StateFlow<Double> = _latitude.asStateFlow()

        private val _longitude = MutableStateFlow(0.0)
        val longitude: StateFlow<Double> = _longitude.asStateFlow()

        private val _speedKmh = MutableStateFlow(0f)
        val speedKmh: StateFlow<Float> = _speedKmh.asStateFlow()

        private val _motionIntensity = MutableStateFlow("UNKNOWN")
        val motionIntensity: StateFlow<String> = _motionIntensity.asStateFlow()

        private val _timeOfDay = MutableStateFlow("UNKNOWN")
        val timeOfDay: StateFlow<String> = _timeOfDay.asStateFlow()

        private val _weather = MutableStateFlow("—")
        val weather: StateFlow<String> = _weather.asStateFlow()

        private val _nowPlayingTitle = MutableStateFlow("")
        val nowPlayingTitle: StateFlow<String> = _nowPlayingTitle.asStateFlow()

        private val _nowPlayingArtist = MutableStateFlow("")
        val nowPlayingArtist: StateFlow<String> = _nowPlayingArtist.asStateFlow()

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

    // Play-to-completion: the latest scene awaiting the next song change
    private val _pendingScene = MutableStateFlow<ClassifiedScene?>(null)
    private var trackEndJob: Job? = null

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
                val key = userPrefs.geminiApiKey
                if (key.isBlank()) {
                    AppLog.w(TAG, "No Gemini API key configured — LLM features will be unavailable")
                }
                GeminiApi(key.ifEmpty { GeminiApi.DEFAULT_API_KEY })
            }
            LlmProviderType.CLAUDE -> {
                val key = userPrefs.claudeApiKey
                if (key.isBlank()) {
                    AppLog.w(TAG, "No Claude API key configured — LLM features will be unavailable")
                }
                ClaudeApi(key.ifEmpty { ClaudeApi.DEFAULT_API_KEY })
            }
            LlmProviderType.OPENAI_COMPATIBLE -> {
                val url = userPrefs.customApiUrl
                val model = userPrefs.customModel
                val key = userPrefs.customApiKey
                if (url.isNotBlank() && model.isNotBlank()) {
                    OpenAiCompatibleApi(baseUrl = url, model = model, apiKey = key)
                } else {
                    val geminiKey = userPrefs.geminiApiKey
                    if (geminiKey.isBlank()) {
                        AppLog.w(TAG, "No API key configured — LLM features will be unavailable")
                    }
                    GeminiApi(geminiKey.ifEmpty { GeminiApi.DEFAULT_API_KEY })
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

        // Main scoring pipeline — PLAY-TO-COMPLETION with urgent interrupt
        //
        // Normal scene changes: store as pending, pick next song when current track ends.
        // Urgent shifts (sprint, sudden departure): interrupt immediately.
        // First song (nothing playing yet): play immediately.

        lifecycleScope.launch {
            try {
                val sceneStates = sensorAggregator.sceneStateFlow()
                val classifiedScenes = contextEngine.classify(sceneStates)

                classifiedScenes
                    .distinctUntilChangedBy { it.classification }
                    .collectLatest { scene ->
                        val classification = scene.classification
                        AppLog.d(TAG, "Scene: $classification (${scene.minutesInScene}min)")
                        _currentScene.value = classification

                        // Publish sensor data to UI
                        _latitude.value = scene.sceneState.latitude
                        _longitude.value = scene.sceneState.longitude
                        _speedKmh.value = scene.sceneState.speedKmh
                        _motionIntensity.value = scene.sceneState.movementIntensity.name
                        _timeOfDay.value = scene.sceneState.timeOfDay.name

                        val placeDesc = buildPlaceDescription(scene.sceneState)
                        if (placeDesc.isNotEmpty()) _placeInfo.value = placeDesc

                        // Check if this is an urgent shift that should interrupt playback
                        val isUrgent = lastClassification?.let { prev ->
                            isUrgentShift(prev, classification)
                        } ?: false

                        // First song (nothing playing) — also treat as immediate
                        val nothingPlaying = currentSongUri == null

                        if (isUrgent || nothingPlaying) {
                            AppLog.d(TAG, "Immediate song pick: urgent=$isUrgent, first=$nothingPlaying")
                            selectAndPlay(scene)
                        } else {
                            // Normal scene change — just update the pending scene.
                            // The track-end watcher will pick a new song when the current one finishes.
                            AppLog.d(TAG, "Scene changed but current song still playing — queued for track end")
                            _pendingScene.value = scene
                        }

                        lastClassification = classification
                    }
            } catch (e: Exception) {
                AppLog.e(TAG, "SCORING PIPELINE CRASHED: ${e.message}", e)
                _matchReason.value = "Pipeline error: ${e.message}"
            }
        }

        // Track-end watcher: when Spotify reports the current track is about to end,
        // pick a new song for the current (or pending) scene.
        trackEndJob?.cancel()
        trackEndJob = lifecycleScope.launch {
            try {
                playbackController.trackEndingSoon
                    .filter { it } // only react when true (track is ending)
                    .collect {
                        val pending = _pendingScene.value
                        if (pending != null) {
                            AppLog.d(TAG, "Track ending — picking next song for pending scene: ${pending.classification}")
                            _pendingScene.value = null
                            selectAndPlay(pending)
                        } else {
                            // No scene change happened, but current track is ending.
                            // Re-score the CURRENT scene to pick a fresh track.
                            AppLog.d(TAG, "Track ending — re-scoring current scene for next track")
                            val currentClassification = _currentScene.value
                            val freshState = sensorAggregator.latestSceneState()
                            if (freshState != null) {
                                val scene = ClassifiedScene(
                                    classification = currentClassification,
                                    sceneState = freshState,
                                    minutesInScene = 0,
                                    sceneStartedAt = java.time.Instant.now()
                                )
                                selectAndPlay(scene)
                            }
                        }
                    }
            } catch (e: Exception) {
                AppLog.e(TAG, "Track-end watcher crashed: ${e.message}", e)
            }
        }
    }

    /** Select the best song for a scene and play it. Used by both immediate and queued paths. */
    private suspend fun selectAndPlay(scene: ClassifiedScene) {
        val classification = scene.classification
        var state = scene.sceneState

        // Inject heart rate
        val hr = heartRateProvider.getLastBpm()
        val hrState = heartRateProvider.getLastState()
        if (hr > 0) {
            state = state.copy(
                heartRateBpm = hr,
                heartRateState = hrState.name.lowercase()
            )
        }

        // Check known location
        val knownLocation = if (state.latitude != 0.0) {
            knownLocationManager.checkLocation(state.latitude, state.longitude, state.placeType)
        } else null

        // Fetch weather
        val weather = if (state.latitude != 0.0) {
            weatherProvider.getWeather(state.latitude, state.longitude)
        } else null

        state = state.copy(weather = weather?.condition)
        _weather.value = weather?.let { "${it.condition} ${it.temperatureC}°C" } ?: "—"

        // Load active character profile — blend mode overrides by time of day
        val characterProfile = if (userPrefs.characterModeEnabled) {
            val characterName = if (userPrefs.blendModeEnabled) {
                userPrefs.getBlendCharacterForTime(state.timeOfDay.name)
            } else {
                userPrefs.activeCharacterName
            }
            if (characterName.isNotBlank()) {
                db.characterProfileDao().getByName(characterName)
            } else null
        } else null

        // Select song (drama + mood + character read live so changes take effect on next pick)
        val selection = narrativeEngine.selectSong(
            sceneState = state,
            classification = classification,
            weather = weather?.let { "${it.condition} (${it.description}, ${it.temperatureC}°C)" },
            knownLocation = knownLocation,
            dramaScale = userPrefs.dramaScale,
            customMood = userPrefs.getActiveMood(),
            characterProfile = characterProfile
        )

        AppLog.d(TAG, "Selected: ${selection.title} by ${selection.artist}")
        _matchReason.value = selection.matchReason
        currentSongUri = selection.spotifyUri

        // Update now playing for UI
        _nowPlayingTitle.value = selection.title
        _nowPlayingArtist.value = selection.artist

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

        // Try to develop leitmotifs
        knownLocation?.let { knownLocationManager.tryDevelopLeitmotif(it.id) }

        // Play with transition
        val isUrgent = lastClassification?.let { prev ->
            isUrgentShift(prev, classification)
        } ?: false

        when (selection.transitionType) {
            "dramatic_silence" -> transitionManager.dramaticSilence(selection.spotifyUri)
            else -> {
                val shift = lastClassification?.let { prev ->
                    ContextShift(from = prev, to = classification, isUrgent = isUrgent)
                }
                transitionManager.transition(selection.spotifyUri, shift)
            }
        }

        updateNotification(classification, selection.title, state.placeType)
    }

    private fun isUrgentShift(from: SceneClassification, to: SceneClassification): Boolean {
        val stationaryTypes = setOf(
            SceneClassification.MORNING_STATIONARY,
            SceneClassification.DAYTIME_STATIONARY,
            SceneClassification.EVENING_STATIONARY,
            SceneClassification.NIGHT_STATIONARY
        )
        if (from in stationaryTypes && to == SceneClassification.TRANSIT) return true
        if (to == SceneClassification.ACTIVE) return true
        return false
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
            onProgress = { analyzed, total ->
                _libraryStatus.value = "Analyzing: $analyzed / $total"
            }
        )

        val suffix = if (!llmProvider.isConfigured) " (basic tags — no API key)" else ""
        _libraryStatus.value = "$count songs ready$suffix"
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
        trackEndJob?.cancel()
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
