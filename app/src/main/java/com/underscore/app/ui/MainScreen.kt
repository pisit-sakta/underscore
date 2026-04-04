package com.underscore.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    versionName: String = "",
    characterColor1: String? = null,
    characterColor2: String? = null,
    characterName: String? = null,
    characterTagline: String? = null,
    characterFranchise: String? = null,
    onStartScoring: () -> Unit,
    onStopScoring: () -> Unit,
    onLogout: () -> Unit,
    onSettings: () -> Unit = {}
) {
    val hasCharacter = characterColor1 != null && characterColor2 != null
    val bgColor = Color(0xFF0A0A0A)

    // Animated colors: smooth 600ms transition when character changes or mode toggles
    val targetColor1 = if (hasCharacter) parseHexColor(characterColor1!!) else bgColor
    val targetColor2 = if (hasCharacter) parseHexColor(characterColor2!!) else bgColor

    val animatedColor1 by animateColorAsState(
        targetValue = targetColor1,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "splitColor1"
    )
    val animatedColor2 by animateColorAsState(
        targetValue = targetColor2,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "splitColor2"
    )

    // Adaptive text color based on the top-left triangle (where header lives)
    val headerTextColor = textColorForBackground(animatedColor1)

    DiagonalSplitBackground(
        color1 = animatedColor1,
        color2 = animatedColor2
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
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
                    color = headerTextColor,
                    letterSpacing = 4.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "OPTIONS",
                        fontSize = 10.sp,
                        color = headerTextColor.copy(alpha = 0.8f),
                        letterSpacing = 1.sp,
                        modifier = Modifier.clickable { onSettings() }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier.size(8.dp)
                    ) {
                        drawCircle(
                            color = if (isSpotifyConnected) Color(0xFF1DB954) else Color.Red
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSpotifyConnected) "ON" else "OFF",
                        fontSize = 10.sp,
                        color = headerTextColor.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )
                }
            }

            if (versionName.isNotEmpty()) {
                Text(
                    text = versionName,
                    fontSize = 10.sp,
                    color = headerTextColor.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(if (characterName != null) 20.dp else 32.dp))

            // Character panel — prominent display when character mode is active
            if (characterName != null && hasCharacter) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Color split preview
                        MiniDiagonalSplit(
                            color1 = animatedColor1,
                            color2 = animatedColor2,
                            modifier = Modifier
                                .height(40.dp)
                                .width(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = characterName.uppercase(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                letterSpacing = 2.sp
                            )
                            if (characterFranchise != null) {
                                Text(
                                    text = characterFranchise,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                    if (characterTagline != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "\"${characterTagline}\"",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Light,
                            color = Color.White.copy(alpha = 0.7f),
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

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

            Spacer(modifier = Modifier.height(24.dp))

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

            Spacer(modifier = Modifier.height(16.dp))
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
    val placeInfo: String = "—",
    val heartRate: String = "—",
    val matchReason: String = "",
    val libraryStatus: String = "Not analyzed"
)

@Composable
fun SensorDebugPanel(info: SensorDebugInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
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
        DebugRow("Heart", info.heartRate)
        DebugRow("Time", info.timeOfDay)
        DebugRow("Weather", info.weather)
        DebugRow("Place", info.placeInfo)
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
