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
import com.underscore.app.context.ContextEngine
import com.underscore.app.context.ContextShift
import com.underscore.app.context.ContextShiftDetector
import com.underscore.app.context.SceneClassification
import com.underscore.app.narrative.SongSelector
import com.underscore.app.playback.PlaybackController
import com.underscore.app.playback.TransitionManager
import com.underscore.app.sensor.SensorAggregator
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
    private lateinit var contextShiftDetector: ContextShiftDetector
    private lateinit var songSelector: SongSelector
    private lateinit var playbackController: PlaybackController
    private lateinit var transitionManager: TransitionManager

    private var lastClassification: SceneClassification? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        sensorAggregator = SensorAggregator(this)
        contextEngine = ContextEngine()
        contextShiftDetector = ContextShiftDetector()
        songSelector = SongSelector()
        playbackController = PlaybackController(this)
        transitionManager = TransitionManager(playbackController)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_STOP) {
            Log.d(TAG, "Stop action received")
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

        // Main pipeline: sensors -> context -> song -> play
        lifecycleScope.launch {
            val sceneStates = sensorAggregator.sceneStateFlow()
            val classifications = contextEngine.classify(sceneStates)

            classifications.collectLatest { classification ->
                Log.d(TAG, "Scene: $classification")
                _currentScene.value = classification

                val shift = lastClassification?.let { prev ->
                    if (prev != classification) {
                        ContextShift(from = prev, to = classification, isUrgent = false)
                    } else null
                }

                if (shift != null || lastClassification == null) {
                    val track = songSelector.selectTrack(classification)
                    Log.d(TAG, "Selected: ${track.title} by ${track.artist}")
                    transitionManager.transition(track.uri, shift)
                    updateNotification(classification, track.title)
                }

                lastClassification = classification
            }
        }
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
        Log.d(TAG, "Service destroyed")
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
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
