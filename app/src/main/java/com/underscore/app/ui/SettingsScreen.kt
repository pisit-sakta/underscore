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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.underscore.app.api.LlmProviderType

@Composable
fun SettingsScreen(
    currentProvider: LlmProviderType,
    geminiKey: String,
    claudeKey: String,
    weatherKey: String,
    placesKey: String,
    batterySaver: Boolean,
    onProviderChanged: (LlmProviderType) -> Unit,
    onGeminiKeyChanged: (String) -> Unit,
    onClaudeKeyChanged: (String) -> Unit,
    onWeatherKeyChanged: (String) -> Unit,
    onPlacesKeyChanged: (String) -> Unit,
    onBatterySaverChanged: (Boolean) -> Unit,
    onDeleteAllData: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BACK",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "SETTINGS",
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 4.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── LLM Provider ──
        SectionHeader("NARRATIVE ENGINE")
        Text(
            text = "Choose who processes your scene data. All data is abstracted — no GPS coordinates or personal info are sent.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        LlmProviderType.entries.forEach { provider ->
            ProviderOption(
                provider = provider,
                isSelected = currentProvider == provider,
                onSelect = { onProviderChanged(provider) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── API Keys ──
        SectionHeader("API KEYS")
        Text(
            text = "Keys are stored locally on your device only.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        ApiKeyField("Gemini API Key", geminiKey, onGeminiKeyChanged)
        Spacer(modifier = Modifier.height(8.dp))
        ApiKeyField("Claude API Key", claudeKey, onClaudeKeyChanged)
        Spacer(modifier = Modifier.height(8.dp))
        ApiKeyField("OpenWeatherMap Key", weatherKey, onWeatherKeyChanged)
        Spacer(modifier = Modifier.height(8.dp))
        ApiKeyField("Google Places Key", placesKey, onPlacesKeyChanged)

        Spacer(modifier = Modifier.height(24.dp))

        // ── Battery ──
        SectionHeader("BATTERY")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Battery Saver", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Reduces sensor polling frequency",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = batterySaver, onCheckedChange = onBatterySaverChanged)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Privacy ──
        SectionHeader("PRIVACY")
        PrivacyPledge()

        Spacer(modifier = Modifier.height(16.dp))

        // Delete all data
        Button(
            onClick = onDeleteAllData,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B))
        ) {
            Text("DELETE ALL DATA", letterSpacing = 2.sp, fontSize = 12.sp)
        }
        Text(
            text = "Removes all local data: song tags, scene history, known locations, preferences.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun ProviderOption(
    provider: LlmProviderType,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onSelect() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onSelect)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = provider.displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = provider.description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ApiKeyField(label: String, value: String, onValueChange: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onValueChange(it)
        },
        label = { Text(label, fontSize = 12.sp) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
    )
}

@Composable
private fun PrivacyPledge() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Text(
            text = "THE UNDERSCORE PRIVACY PLEDGE",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        val pledgeItems = listOf(
            "No data goes to our servers. We don't have data servers.",
            "Location, heart rate, movement — stays on YOUR device.",
            "The LLM receives abstracted scene data only. No GPS. No user ID.",
            "You choose your LLM provider. You control the trust tradeoff.",
            "Airplane mode works. Turn off network and verify.",
            "One-button data deletion. Settings > Delete All Data. Gone."
        )
        pledgeItems.forEach { item ->
            Text(
                text = "- $item",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}
