package com.underscore.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.underscore.app.context.SceneClassification
import com.underscore.app.playback.NowPlaying

@Composable
fun NowPlayingCard(
    nowPlaying: NowPlaying,
    scene: SceneClassification,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp)
    ) {
        Text(
            text = sceneDisplayName(scene),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.secondary,
            letterSpacing = 3.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (nowPlaying.trackName.isNotEmpty()) {
            Text(
                text = nowPlaying.trackName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = nowPlaying.artistName,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "Waiting for context...",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun sceneDisplayName(scene: SceneClassification): String = when (scene) {
    SceneClassification.MORNING_STATIONARY -> "MORNING"
    SceneClassification.DAYTIME_STATIONARY -> "DAYTIME"
    SceneClassification.EVENING_STATIONARY -> "EVENING"
    SceneClassification.NIGHT_STATIONARY -> "NIGHT"
    SceneClassification.TRANSIT -> "TRANSIT"
    SceneClassification.WALKING -> "WALKING"
    SceneClassification.ACTIVE -> "ACTIVE"
    SceneClassification.UNKNOWN -> "DETECTING..."
}
