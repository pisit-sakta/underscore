package com.underscore.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
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
import androidx.core.content.ContextCompat
import com.underscore.app.auth.SpotifyAuth
import com.underscore.app.data.SongDatabase
import com.underscore.app.data.UserPreferences
import com.underscore.app.debug.LogCollector
import com.underscore.app.playback.PlaybackController
import com.underscore.app.service.UnderscoreService
import com.underscore.app.ui.LoginScreen
import com.underscore.app.ui.MainScreen
import com.underscore.app.ui.SensorDebugInfo
import com.underscore.app.ui.SettingsScreen
import com.underscore.app.ui.SettingsState
import com.underscore.app.ui.theme.UnderscoreTheme
import com.underscore.app.updater.AppUpdater
import com.underscore.app.updater.DownloadState
import com.underscore.app.updater.UpdateInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var spotifyAuth: SpotifyAuth
    private lateinit var playbackController: PlaybackController
    private lateinit var appUpdater: AppUpdater
    private lateinit var userPrefs: UserPreferences

    private var sensorDebug by mutableStateOf(SensorDebugInfo())
    private var pendingUpdate by mutableStateOf<UpdateInfo?>(null)
    private var showSettings by mutableStateOf(false)

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

                val isLoggedIn = spotifyAuth.isLoggedIn()
                val isScoring by UnderscoreService.isRunning.collectAsState()
                val currentScene by UnderscoreService.currentScene.collectAsState()
                val nowPlaying by playbackController.nowPlaying.collectAsState()
                val isSpotifyConnected by playbackController.isConnected.collectAsState()
                val matchReason by UnderscoreService.matchReason.collectAsState()
                val libraryStatus by UnderscoreService.libraryStatus.collectAsState()
                val placeInfo by UnderscoreService.placeInfo.collectAsState()
                val heartRate by UnderscoreService.heartRate.collectAsState()

                when {
                    !isLoggedIn -> {
                        LoginScreen(
                            onConnectSpotify = { spotifyAuth.startAuth(this@MainActivity) }
                        )
                    }
                    showSettings -> {
                        SettingsScreen(
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
                                // Restart service so it picks up the new provider
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
                            onBack = { showSettings = false }
                        )
                    }
                    else -> {
                        MainScreen(
                            isScoring = isScoring,
                            isSpotifyConnected = isSpotifyConnected,
                            nowPlaying = nowPlaying,
                            currentScene = currentScene,
                            sensorDebug = SensorDebugInfo(
                                scene = currentScene.name,
                                placeInfo = placeInfo.ifEmpty { "—" },
                                heartRate = if (heartRate > 0) "$heartRate bpm" else "—",
                                matchReason = matchReason,
                                libraryStatus = libraryStatus
                            ),
                            versionName = getVersionName(),
                            onStartScoring = { startScoring() },
                            onStopScoring = { stopScoring() },
                            onLogout = { logout() },
                            onSettings = { showSettings = true }
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
