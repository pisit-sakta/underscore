package com.underscore.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class MoodDuration(val label: String, val ms: Long) {
    UNTIL_CLEARED("Until I change it", 0L),
    TWO_HOURS("Next 2 hours", 2 * 60 * 60 * 1000L),
    TODAY("Today", 12 * 60 * 60 * 1000L)
}

private val MOOD_PRESETS = listOf(
    "Confident", "Melancholic", "Motivated", "Rainy Day",
    "Late Night", "Villain Arc", "Main Character", "Vibing"
)

@Composable
fun MoodSubScreen(
    currentMood: String,
    moodExpiresAt: Long,
    onMoodSet: (String, Long) -> Unit,
    onMoodCleared: () -> Unit,
    onBack: () -> Unit
) {
    var moodText by remember(currentMood) { mutableStateOf(currentMood) }
    var selectedDuration by remember { mutableStateOf(MoodDuration.UNTIL_CLEARED) }
    val hasMood = currentMood.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SubScreenHeader("VIBE CHECK", onBack)

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
        ) {
            if (hasMood) {
                Text(
                    text = "Current vibe:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = currentMood,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )
                if (moodExpiresAt > 0) {
                    val remaining = (moodExpiresAt - System.currentTimeMillis()) / 60000
                    if (remaining > 0) {
                        Text(
                            text = "Expires in ~${remaining}min",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { moodText = ""; onMoodCleared() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CLEAR MOOD", fontSize = 12.sp, letterSpacing = 1.sp)
                }
            } else {
                Text(
                    text = "How are you feeling? Type anything.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Free text input
            OutlinedTextField(
                value = moodText,
                onValueChange = { moodText = it },
                label = { Text("Your vibe", fontSize = 12.sp) },
                placeholder = {
                    Text(
                        "Ryan Gosling, villain origin story, 2am convenience store...",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick presets
            SectionHeader("QUICK VIBES")

            for (row in MOOD_PRESETS.chunked(4)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { preset ->
                        Text(
                            text = preset,
                            fontSize = 11.sp,
                            color = if (moodText == preset) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (moodText == preset) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (moodText == preset) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { moodText = preset }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Duration selector
            SectionHeader("DURATION")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MoodDuration.entries.forEach { duration ->
                    Text(
                        text = duration.label,
                        fontSize = 11.sp,
                        color = if (selectedDuration == duration) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selectedDuration == duration) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (selectedDuration == duration) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { selectedDuration = duration }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Set mood button
            Button(
                onClick = {
                    if (moodText.isNotBlank()) {
                        onMoodSet(moodText, selectedDuration.ms)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = moodText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
            ) {
                Text("SET VIBE", letterSpacing = 2.sp, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
