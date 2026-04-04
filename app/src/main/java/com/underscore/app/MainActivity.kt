package com.underscore.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.underscore.app.auth.SpotifyAuth
import com.underscore.app.data.CharacterProfile
import com.underscore.app.data.DramaScale
import com.underscore.app.data.FranchiseProfile
import com.underscore.app.data.PresetCharacters
import com.underscore.app.data.SongDatabase
import com.underscore.app.data.UserPreferences
import com.underscore.app.narrative.CharacterGenerator
import com.underscore.app.ui.AppScreen
import com.underscore.app.ui.CharacterSubScreen
import com.underscore.app.ui.ColorHarmonyValidator
import com.underscore.app.ui.DramaSubScreen
import com.underscore.app.ui.FranchiseSubScreen
import com.underscore.app.ui.MoodSubScreen
import com.underscore.app.ui.OptionsMenuScreen
import com.underscore.app.ui.SettingsSubScreen
import com.underscore.app.debug.LogCollector
import com.underscore.app.playback.NowPlaying
import com.underscore.app.playback.PlaybackController
import com.underscore.app.service.UnderscoreService
import com.underscore.app.ui.LoginScreen
import com.underscore.app.ui.MainScreen
import com.underscore.app.ui.SensorDebugInfo
import com.underscore.app.ui.SettingsState
import com.underscore.app.ui.theme.UnderscoreTheme
import com.underscore.app.updater.AppUpdater
import com.underscore.app.updater.DownloadState
import com.underscore.app.updater.UpdateInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var spotifyAuth: SpotifyAuth
    private lateinit var playbackController: PlaybackController
    private lateinit var appUpdater: AppUpdater
    private lateinit var userPrefs: UserPreferences

    private var sensorDebug by mutableStateOf(SensorDebugInfo())
    private var pendingUpdate by mutableStateOf<UpdateInfo?>(null)
    private var currentScreen by mutableStateOf<AppScreen>(AppScreen.Main)
    private var showSpotifyHint by mutableStateOf(false)
    private var characterList by mutableStateOf<List<CharacterProfile>>(emptyList())
    private var isGeneratingCharacter by mutableStateOf(false)
    private var activeCharacterProfile by mutableStateOf<CharacterProfile?>(null)
    private var isGeneratingFranchise by mutableStateOf(false)
    private var activeFranchiseProfile by mutableStateOf<FranchiseProfile?>(null)
    private var franchiseError by mutableStateOf<String?>(null)

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        spotifyAuth = SpotifyAuth(this)
        playbackController = PlaybackController(this)
        appUpdater = AppUpdater(this)
        userPrefs = UserPreferences(this)

        requestPermissions()
        checkForUpdate()

        // Install preset characters + load character list
        val db = SongDatabase.getInstance(this)
        CoroutineScope(Dispatchers.IO).launch {
            PresetCharacters.installPresets(db.characterProfileDao())
            characterList = db.characterProfileDao().getAll()
            // Load active character if set
            val activeName = userPrefs.activeCharacterName
            if (activeName.isNotBlank() && userPrefs.characterModeEnabled) {
                activeCharacterProfile = db.characterProfileDao().getByName(activeName)
            }
            // Load active franchise if set
            if (userPrefs.franchiseImmersionEnabled) {
                activeFranchiseProfile = FranchiseProfile.fromJson(userPrefs.activeFranchiseJson)
            }
        }

        // Force re-login if Spotify scopes changed (e.g. added playback control)
        if (spotifyAuth.isLoggedIn() && userPrefs.needsSpotifyRelogin()) {
            spotifyAuth.logout()
        }

        if (!userPrefs.spotifyHintDismissed && spotifyAuth.isLoggedIn()) {
            showSpotifyHint = true
        }

        // Handle Spotify auth redirect if we were launched via underscore://spotify-auth-callback
        handleSpotifyRedirect(intent)

        setContent {
            UnderscoreTheme {
                val dlProgress by appUpdater.downloadProgress.collectAsState()

                // Update dialog
                pendingUpdate?.let { update ->
                    val isDownloading = dlProgress.state == DownloadState.DOWNLOADING
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = {
                            if (!isDownloading) {
                                appUpdater.dismissUpdate(update.buildNumber)
                                pendingUpdate = null
                            }
                        },
                        title = { Text("Update Available") },
                        text = {
                            Column {
                                when (dlProgress.state) {
                                    DownloadState.IDLE -> Text(
                                        "${update.releaseName}\n\nA new version is ready. Update now?"
                                    )
                                    DownloadState.DOWNLOADING -> {
                                        Text("Downloading... ${dlProgress.percent}%")
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LinearProgressIndicator(
                                            progress = dlProgress.percent / 100f,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    DownloadState.INSTALLING -> Text("Installing...")
                                    DownloadState.FAILED -> Text(
                                        "Download failed: ${dlProgress.error ?: "Unknown error"}\n\nTry again?"
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            if (dlProgress.state == DownloadState.IDLE || dlProgress.state == DownloadState.FAILED) {
                                androidx.compose.material3.TextButton(onClick = {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        appUpdater.downloadAndInstall(update)
                                    }
                                }) {
                                    Text(if (dlProgress.state == DownloadState.FAILED) "RETRY" else "UPDATE")
                                }
                            }
                        },
                        dismissButton = {
                            if (!isDownloading) {
                                androidx.compose.material3.TextButton(onClick = {
                                    appUpdater.dismissUpdate(update.buildNumber)
                                    pendingUpdate = null
                                }) {
                                    Text("LATER")
                                }
                            }
                        }
                    )
                }

                // Spotify hint dialog
                if (showSpotifyHint) {
                    var doNotShowAgain by androidx.compose.runtime.remember { mutableStateOf(false) }
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = {},
                        title = { Text("Before You Start") },
                        text = {
                            Column {
                                Text(
                                    "Make sure Spotify is open and playing on your phone before starting scoring.\n\n" +
                                    "Underscore controls playback on whatever device Spotify is active on. " +
                                    "If no active device is found, it will try to find one automatically."
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    androidx.compose.material3.Checkbox(
                                        checked = doNotShowAgain,
                                        onCheckedChange = { doNotShowAgain = it }
                                    )
                                    Text(
                                        text = "Don't show this again",
                                        fontSize = 14.sp,
                                        modifier = Modifier.clickable { doNotShowAgain = !doNotShowAgain }
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                if (doNotShowAgain) {
                                    userPrefs.spotifyHintDismissed = true
                                }
                                showSpotifyHint = false
                            }) {
                                Text("OK")
                            }
                        }
                    )
                }

                val isLoggedIn = spotifyAuth.isLoggedIn()
                val isScoring by UnderscoreService.isRunning.collectAsState()
                val currentScene by UnderscoreService.currentScene.collectAsState()
                val spotifyNowPlaying by playbackController.nowPlaying.collectAsState()
                val serviceTitle by UnderscoreService.nowPlayingTitle.collectAsState()
                val serviceArtist by UnderscoreService.nowPlayingArtist.collectAsState()
                // Prefer Spotify's actual now playing, fall back to service selection
                val nowPlaying = if (spotifyNowPlaying.trackName.isNotEmpty()) {
                    spotifyNowPlaying
                } else {
                    NowPlaying(trackName = serviceTitle, artistName = serviceArtist)
                }
                val isSpotifyConnected by playbackController.isConnected.collectAsState()
                val matchReason by UnderscoreService.matchReason.collectAsState()
                val libraryStatus by UnderscoreService.libraryStatus.collectAsState()
                val placeInfo by UnderscoreService.placeInfo.collectAsState()
                val heartRate by UnderscoreService.heartRate.collectAsState()
                val sensorLat by UnderscoreService.latitude.collectAsState()
                val sensorLng by UnderscoreService.longitude.collectAsState()
                val sensorSpeed by UnderscoreService.speedKmh.collectAsState()
                val sensorMotion by UnderscoreService.motionIntensity.collectAsState()
                val sensorTime by UnderscoreService.timeOfDay.collectAsState()
                val sensorWeather by UnderscoreService.weather.collectAsState()

                if (!isLoggedIn) {
                    LoginScreen(
                        onConnectSpotify = { spotifyAuth.startAuth(this@MainActivity) }
                    )
                } else when (currentScreen) {
                    AppScreen.OptionsMenu -> {
                        OptionsMenuScreen(
                            providerName = userPrefs.llmProvider.displayName,
                            characterSummary = if (userPrefs.characterModeEnabled)
                                userPrefs.activeCharacterName.ifBlank { "Enabled" } else "Off",
                            franchiseSummary = if (userPrefs.franchiseImmersionEnabled)
                                activeFranchiseProfile?.let {
                                    if (it.mood.isNotBlank()) "${it.name} (${it.mood})" else it.name
                                } ?: "Enabled" else "Off",
                            moodSummary = userPrefs.getActiveMood()?.ifBlank { null }
                                ?: "No active vibe",
                            dramaSummary = "${userPrefs.dramaScale} — ${DramaScale.getDisplayName(userPrefs.dramaScale, userPrefs.foodAnalogyMode)}",
                            onSettingsClick = { currentScreen = AppScreen.Settings },
                            onCharacterClick = { currentScreen = AppScreen.Character },
                            onFranchiseClick = { currentScreen = AppScreen.Franchise },
                            onMoodClick = { currentScreen = AppScreen.Mood },
                            onDramaClick = { currentScreen = AppScreen.Drama },
                            onBack = { currentScreen = AppScreen.Main }
                        )
                    }
                    AppScreen.Settings -> {
                        SettingsSubScreen(
                            state = SettingsState(
                                provider = userPrefs.llmProvider,
                                geminiKey = userPrefs.geminiApiKey,
                                claudeKey = userPrefs.claudeApiKey,
                                customApiUrl = userPrefs.customApiUrl,
                                customApiKey = userPrefs.customApiKey,
                                customModel = userPrefs.customModel,
                                weatherKey = userPrefs.weatherApiKey,
                                placesKey = userPrefs.placesApiKey,
                                batterySaver = userPrefs.batterySaver
                            ),
                            onProviderChanged = {
                                userPrefs.llmProvider = it
                                if (UnderscoreService.isRunning.value) {
                                    stopScoring()
                                    startScoring()
                                }
                            },
                            onGeminiKeyChanged = { userPrefs.geminiApiKey = it },
                            onClaudeKeyChanged = { userPrefs.claudeApiKey = it },
                            onCustomApiUrlChanged = { userPrefs.customApiUrl = it },
                            onCustomApiKeyChanged = { userPrefs.customApiKey = it },
                            onCustomModelChanged = { userPrefs.customModel = it },
                            onWeatherKeyChanged = { userPrefs.weatherApiKey = it },
                            onPlacesKeyChanged = { userPrefs.placesApiKey = it },
                            onBatterySaverChanged = { userPrefs.batterySaver = it },
                            onDeleteAllData = { deleteAllData() },
                            onShareDebugReport = { LogCollector(this@MainActivity).reportBug() },
                            onBack = { currentScreen = AppScreen.OptionsMenu }
                        )
                    }
                    AppScreen.Character -> {
                        CharacterSubScreen(
                            characterModeEnabled = userPrefs.characterModeEnabled,
                            activeCharacterName = userPrefs.activeCharacterName,
                            characters = characterList,
                            isGeneratingCharacter = isGeneratingCharacter,
                            onCharacterModeChanged = { enabled ->
                                userPrefs.characterModeEnabled = enabled
                                if (!enabled) activeCharacterProfile = null
                                else {
                                    val name = userPrefs.activeCharacterName
                                    if (name.isNotBlank()) {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            activeCharacterProfile = db.characterProfileDao().getByName(name)
                                        }
                                    }
                                }
                            },
                            onCharacterSelected = { name ->
                                userPrefs.activeCharacterName = name
                                CoroutineScope(Dispatchers.IO).launch {
                                    activeCharacterProfile = db.characterProfileDao().getByName(name)
                                }
                            },
                            onGenerateCharacter = { name ->
                                isGeneratingCharacter = true
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val generator = CharacterGenerator(createLlmProvider())
                                        val profile = generator.generate(name)
                                        if (profile != null) {
                                            val validated = ColorHarmonyValidator.validate(profile.color1, profile.color2)
                                            val finalProfile = if (validated.wasModified) {
                                                com.underscore.app.debug.AppLog.d("MainActivity",
                                                    "Color harmony corrected for ${profile.name}: " +
                                                    "${profile.color1}/${profile.color2} -> ${validated.color1}/${validated.color2}")
                                                profile.copy(color1 = validated.color1, color2 = validated.color2)
                                            } else profile
                                            db.characterProfileDao().insert(finalProfile)
                                            characterList = db.characterProfileDao().getAll()
                                            userPrefs.activeCharacterName = finalProfile.name
                                            activeCharacterProfile = finalProfile
                                        }
                                    } catch (e: Exception) {
                                        com.underscore.app.debug.AppLog.e("MainActivity", "Character generation failed: ${e.message}", e)
                                    } finally {
                                        isGeneratingCharacter = false
                                    }
                                }
                            },
                            onBack = { currentScreen = AppScreen.OptionsMenu }
                        )
                    }
                    AppScreen.Franchise -> {
                        FranchiseSubScreen(
                            franchiseEnabled = userPrefs.franchiseImmersionEnabled,
                            activeFranchise = activeFranchiseProfile,
                            isGenerating = isGeneratingFranchise,
                            errorMessage = franchiseError,
                            onToggle = { enabled ->
                                userPrefs.franchiseImmersionEnabled = enabled
                                if (!enabled) activeFranchiseProfile = null
                            },
                            onGenerate = { input ->
                                isGeneratingFranchise = true
                                franchiseError = null
                                CoroutineScope(Dispatchers.Main).launch {
                                    try {
                                        val result = withContext(Dispatchers.IO) {
                                            val generator = CharacterGenerator(createLlmProvider())
                                            generator.generateFranchise(input)
                                        }
                                        // Back on Main thread — safe to update Compose state
                                        if (result.profile != null) {
                                            val profile = result.profile
                                            val validated = ColorHarmonyValidator.validate(profile.color1, profile.color2)
                                            val finalProfile = if (validated.wasModified) {
                                                com.underscore.app.debug.AppLog.d("MainActivity",
                                                    "Franchise color harmony corrected: ${profile.color1}/${profile.color2} -> ${validated.color1}/${validated.color2}")
                                                profile.copy(color1 = validated.color1, color2 = validated.color2)
                                            } else profile
                                            userPrefs.activeFranchiseJson = finalProfile.toJson()
                                            userPrefs.franchiseImmersionEnabled = true
                                            activeFranchiseProfile = finalProfile
                                            franchiseError = null
                                        } else {
                                            franchiseError = result.error ?: "Unknown error generating franchise profile"
                                        }
                                    } catch (e: Exception) {
                                        com.underscore.app.debug.AppLog.e("MainActivity", "Franchise generation failed: ${e.message}", e)
                                        franchiseError = "Generation failed: ${e.message}"
                                    } finally {
                                        isGeneratingFranchise = false
                                    }
                                }
                            },
                            onBack = { currentScreen = AppScreen.OptionsMenu }
                        )
                    }
                    AppScreen.Mood -> {
                        MoodSubScreen(
                            currentMood = userPrefs.getActiveMood() ?: "",
                            moodExpiresAt = userPrefs.moodExpiresAt,
                            onMoodSet = { mood, durationMs ->
                                userPrefs.setMoodWithDuration(mood, durationMs)
                            },
                            onMoodCleared = { userPrefs.clearMood() },
                            onBack = { currentScreen = AppScreen.OptionsMenu }
                        )
                    }
                    AppScreen.Drama -> {
                        DramaSubScreen(
                            dramaScale = userPrefs.dramaScale,
                            foodAnalogyMode = userPrefs.foodAnalogyMode,
                            onDramaScaleChanged = { userPrefs.dramaScale = it },
                            onFoodAnalogyChanged = { userPrefs.foodAnalogyMode = it },
                            onBack = { currentScreen = AppScreen.OptionsMenu }
                        )
                    }
                    else -> {
                        MainScreen(
                            isScoring = isScoring,
                            isSpotifyConnected = isSpotifyConnected,
                            nowPlaying = nowPlaying,
                            currentScene = currentScene,
                            sensorDebug = SensorDebugInfo(
                                latitude = sensorLat,
                                longitude = sensorLng,
                                speedKmh = sensorSpeed,
                                movementIntensity = sensorMotion,
                                timeOfDay = sensorTime,
                                scene = currentScene.name,
                                weather = sensorWeather,
                                placeInfo = placeInfo.ifEmpty { "—" },
                                heartRate = if (heartRate > 0) "$heartRate bpm" else "—",
                                matchReason = matchReason,
                                libraryStatus = libraryStatus
                            ),
                            versionName = getVersionName(),
                            characterColor1 = when {
                                userPrefs.characterModeEnabled -> activeCharacterProfile?.color1
                                userPrefs.franchiseImmersionEnabled -> activeFranchiseProfile?.color1
                                else -> null
                            },
                            characterColor2 = when {
                                userPrefs.characterModeEnabled -> activeCharacterProfile?.color2
                                userPrefs.franchiseImmersionEnabled -> activeFranchiseProfile?.color2
                                else -> null
                            },
                            characterName = when {
                                userPrefs.characterModeEnabled -> activeCharacterProfile?.name
                                userPrefs.franchiseImmersionEnabled -> activeFranchiseProfile?.let {
                                    if (it.mood.isNotBlank()) "${it.name} (${it.mood})" else it.name
                                }
                                else -> null
                            },
                            characterTagline = if (userPrefs.characterModeEnabled) activeCharacterProfile?.tagline else null,
                            characterFranchise = when {
                                userPrefs.characterModeEnabled -> activeCharacterProfile?.franchise
                                userPrefs.franchiseImmersionEnabled -> activeFranchiseProfile?.aesthetic?.take(60)
                                else -> null
                            },
                            onStartScoring = { startScoring() },
                            onStopScoring = { stopScoring() },
                            onLogout = { logout() },
                            onSettings = { currentScreen = AppScreen.OptionsMenu }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSpotifyRedirect(intent)
    }

    private fun handleSpotifyRedirect(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "underscore" && uri.host == "spotify-auth-callback") {
            val hasCode = spotifyAuth.handleRedirectUri(uri)
            if (hasCode) {
                // PKCE flow: we have the auth code, now exchange it for tokens
                CoroutineScope(Dispatchers.Main).launch {
                    val success = spotifyAuth.exchangeCodeForToken()
                    if (success) {
                        playbackController.connect()
                        recreate()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (spotifyAuth.isLoggedIn()) {
            playbackController.connect()
        }
    }

    override fun onStop() {
        super.onStop()
        playbackController.disconnect()
    }

    private fun createLlmProvider(): com.underscore.app.api.LlmProvider {
        return when (userPrefs.llmProvider) {
            com.underscore.app.api.LlmProviderType.GEMINI -> {
                val key = userPrefs.geminiApiKey.ifEmpty { com.underscore.app.api.GeminiApi.DEFAULT_API_KEY }
                com.underscore.app.api.GeminiApi(key)
            }
            com.underscore.app.api.LlmProviderType.CLAUDE -> {
                val key = userPrefs.claudeApiKey.ifEmpty { com.underscore.app.api.ClaudeApi.DEFAULT_API_KEY }
                com.underscore.app.api.ClaudeApi(key)
            }
            com.underscore.app.api.LlmProviderType.OPENAI_COMPATIBLE -> {
                val url = userPrefs.customApiUrl
                val model = userPrefs.customModel
                val key = userPrefs.customApiKey
                if (url.isNotBlank() && model.isNotBlank()) {
                    com.underscore.app.api.OpenAiCompatibleApi(baseUrl = url, model = model, apiKey = key)
                } else {
                    val geminiKey = userPrefs.geminiApiKey.ifEmpty { com.underscore.app.api.GeminiApi.DEFAULT_API_KEY }
                    com.underscore.app.api.GeminiApi(geminiKey)
                }
            }
        }
    }

    private fun startScoring() {
        UnderscoreService.start(this)
    }

    private fun stopScoring() {
        UnderscoreService.stop(this)
    }

    private fun logout() {
        stopScoring()
        playbackController.disconnect()
        spotifyAuth.logout()
        recreate()
    }

    private fun deleteAllData() {
        CoroutineScope(Dispatchers.IO).launch {
            SongDatabase.getInstance(this@MainActivity).taggedSongDao().deleteAll()
            userPrefs.deleteAllData()
        }
        stopScoring()
        recreate()
    }

    private fun getVersionName(): String {
        return try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName ?: "unknown"
        } catch (e: Exception) { "unknown" }
    }

    private fun checkForUpdate() {
        CoroutineScope(Dispatchers.Main).launch {
            val update = appUpdater.checkForUpdate()
            if (update != null) {
                pendingUpdate = update
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Heart rate sensor permission (Wear OS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            permissions.add(Manifest.permission.BODY_SENSORS)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            locationPermissionLauncher.launch(needed.toTypedArray())
        }
    }
}
