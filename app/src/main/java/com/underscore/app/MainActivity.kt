package com.underscore.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.underscore.app.auth.SpotifyAuth
import com.underscore.app.context.SceneClassification
import com.underscore.app.playback.NowPlaying
import com.underscore.app.playback.PlaybackController
import com.underscore.app.service.UnderscoreService
import com.underscore.app.ui.LoginScreen
import com.underscore.app.ui.MainScreen
import com.underscore.app.ui.SensorDebugInfo
import com.underscore.app.ui.theme.UnderscoreTheme

class MainActivity : ComponentActivity() {

    private lateinit var spotifyAuth: SpotifyAuth
    private lateinit var playbackController: PlaybackController

    // Debug sensor state — updated by service broadcasts (simplified for Sprint 0)
    private var sensorDebug by mutableStateOf(SensorDebugInfo())

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (fineGranted) {
            // Permissions granted — user can now start scoring
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        spotifyAuth = SpotifyAuth(this)
        playbackController = PlaybackController(this)

        requestPermissions()

        setContent {
            UnderscoreTheme {
                val isLoggedIn = spotifyAuth.isLoggedIn()
                val isScoring by UnderscoreService.isRunning.collectAsState()
                val currentScene by UnderscoreService.currentScene.collectAsState()
                val nowPlaying by playbackController.nowPlaying.collectAsState()
                val isSpotifyConnected by playbackController.isConnected.collectAsState()

                if (!isLoggedIn) {
                    LoginScreen(
                        onConnectSpotify = { spotifyAuth.startAuth(this@MainActivity) }
                    )
                } else {
                    MainScreen(
                        isScoring = isScoring,
                        isSpotifyConnected = isSpotifyConnected,
                        nowPlaying = nowPlaying,
                        currentScene = currentScene,
                        sensorDebug = SensorDebugInfo(
                            scene = currentScene.name
                        ),
                        onStartScoring = { startScoring() },
                        onStopScoring = { stopScoring() },
                        onLogout = { logout() }
                    )
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

    @Deprecated("Use Activity Result APIs")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SpotifyAuth.AUTH_REQUEST_CODE) {
            val success = spotifyAuth.handleAuthResponse(resultCode, data)
            if (success) {
                playbackController.connect()
                // Force recompose
                recreate()
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

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            locationPermissionLauncher.launch(needed.toTypedArray())
        }
    }
}
