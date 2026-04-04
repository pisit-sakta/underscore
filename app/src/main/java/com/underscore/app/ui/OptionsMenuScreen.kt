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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OptionsMenuScreen(
    providerName: String,
    characterSummary: String,
    franchiseSummary: String,
    moodSummary: String,
    dramaSummary: String,
    onSettingsClick: () -> Unit,
    onCharacterClick: () -> Unit,
    onFranchiseClick: () -> Unit,
    onMoodClick: () -> Unit,
    onDramaClick: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        SubScreenHeader("OPTIONS", onBack)

        Spacer(modifier = Modifier.height(32.dp))

        OptionItem(
            title = "Settings",
            description = "LLM provider, API keys, battery, privacy",
            subtitle = providerName,
            onClick = onSettingsClick
        )

        Spacer(modifier = Modifier.height(12.dp))

        OptionItem(
            title = "Character",
            description = "Embody a specific character's emotional architecture",
            subtitle = characterSummary,
            onClick = onCharacterClick
        )

        Spacer(modifier = Modifier.height(12.dp))

        OptionItem(
            title = "Franchise",
            description = "Immerse in a franchise's full soundtrack palette",
            subtitle = franchiseSummary,
            onClick = onFranchiseClick
        )

        Spacer(modifier = Modifier.height(12.dp))

        OptionItem(
            title = "Mood",
            description = "Set your vibe, presets, duration",
            subtitle = moodSummary,
            onClick = onMoodClick
        )

        Spacer(modifier = Modifier.height(12.dp))

        OptionItem(
            title = "Drama",
            description = "Intensity level, food analogy mode",
            subtitle = dramaSummary,
            onClick = onDramaClick
        )
    }
}

@Composable
private fun OptionItem(
    title: String,
    description: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = ">",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
