package com.underscore.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.underscore.app.context.SceneClassification
import com.underscore.app.playback.NowPlaying

@Composable
fun MainScreen(
    isScoring: Boolean,
    isSpotifyConnected: Boolean,
    nowPlaying: NowPlaying,
    currentScene: SceneClassification,
    sensorDebug: SensorDebugInfo,
    onStartScoring: () -> Unit,
    onStopScoring: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "_ UNDERSCORE",
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 4.sp
            )

            // Connection status dot
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.size(8.dp)
                ) {
                    drawCircle(
                        color = if (isSpotifyConnected) Color(0xFF1DB954) else Color.Red
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSpotifyConnected) "CONNECTED" else "DISCONNECTED",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Now Playing
        NowPlayingCard(
            nowPlaying = nowPlaying,
            scene = currentScene
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Start/Stop Button
        Button(
            onClick = if (isScoring) onStopScoring else onStartScoring,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isScoring) Color(0xFF333333) else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (isScoring) "STOP SCORING" else "START SCORING",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 3.sp,
                color = if (isScoring) Color.White else Color.Black
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Debug Info
        SensorDebugPanel(sensorDebug)

        Spacer(modifier = Modifier.weight(1f))

        // Logout
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "DISCONNECT SPOTIFY",
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

data class SensorDebugInfo(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speedKmh: Float = 0f,
    val movementIntensity: String = "UNKNOWN",
    val timeOfDay: String = "UNKNOWN",
    val scene: String = "UNKNOWN",
    val weather: String = "—",
    val matchReason: String = "",
    val libraryStatus: String = "Not analyzed"
)

@Composable
fun SensorDebugPanel(info: SensorDebugInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Text(
            text = "SENSOR DEBUG",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        DebugRow("GPS", "%.4f, %.4f".format(info.latitude, info.longitude))
        DebugRow("Speed", "%.1f km/h".format(info.speedKmh))
        DebugRow("Motion", info.movementIntensity)
        DebugRow("Time", info.timeOfDay)
        DebugRow("Weather", info.weather)
        DebugRow("Scene", info.scene)
        DebugRow("Library", info.libraryStatus)
        if (info.matchReason.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = info.matchReason,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.secondary,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun DebugRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
