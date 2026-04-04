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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.underscore.app.api.LlmProviderType

data class SettingsState(
    val provider: LlmProviderType = LlmProviderType.GEMINI,
    val geminiKey: String = "",
    val claudeKey: String = "",
    val customApiUrl: String = "",
    val customApiKey: String = "",
    val customModel: String = "",
    val weatherKey: String = "",
    val placesKey: String = "",
    val batterySaver: Boolean = false
)

@Composable
fun SettingsScreen(
    state: SettingsState,
    onProviderChanged: (LlmProviderType) -> Unit,
    onGeminiKeyChanged: (String) -> Unit,
    onClaudeKeyChanged: (String) -> Unit,
    onCustomApiUrlChanged: (String) -> Unit,
    onCustomApiKeyChanged: (String) -> Unit,
    onCustomModelChanged: (String) -> Unit,
    onWeatherKeyChanged: (String) -> Unit,
    onPlacesKeyChanged: (String) -> Unit,
    onBatterySaverChanged: (Boolean) -> Unit,
    onDeleteAllData: () -> Unit,
    onShareDebugReport: () -> Unit,
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
                text = "< BACK",
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
                isSelected = state.provider == provider,
                onSelect = { onProviderChanged(provider) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Provider-specific config ──
        when (state.provider) {
            LlmProviderType.GEMINI -> {
                SectionHeader("GEMINI CONFIG")
                ApiKeyField("Google AI API Key", state.geminiKey, onGeminiKeyChanged)
                SetupGuide(
                    title = "How to get a Gemini key (free)",
                    steps = listOf(
                        "1. Go to aistudio.google.com",
                        "2. Sign in with your Google account",
                        "3. Click \"Get API key\" in the top left",
                        "4. Click \"Create API key\"",
                        "5. Copy the key and paste it above"
                    ),
                    note = "Free tier: 15 requests/minute. More than enough for Underscore."
                )
            }
            LlmProviderType.CLAUDE -> {
                SectionHeader("CLAUDE CONFIG")
                ApiKeyField("Anthropic API Key", state.claudeKey, onClaudeKeyChanged)
                SetupGuide(
                    title = "How to get a Claude key",
                    steps = listOf(
                        "1. Go to console.anthropic.com",
                        "2. Sign up or sign in",
                        "3. Go to Settings > API Keys",
                        "4. Click \"Create Key\"",
                        "5. Copy the key and paste it above"
                    ),
                    note = "Requires adding credits (\$5 minimum). Strong privacy stance."
                )
            }
            LlmProviderType.OPENAI_COMPATIBLE -> {
                SectionHeader("CUSTOM ENDPOINT")
                HintText("Any OpenAI-compatible API: OpenRouter, local LLMs, proxy servers, self-hosted models. Enter the base URL, model ID, and API key.")

                Spacer(modifier = Modifier.height(8.dp))

                TextInputField(
                    label = "API URL",
                    value = state.customApiUrl,
                    onValueChange = onCustomApiUrlChanged,
                    placeholder = "https://my-proxy.example.com/v1"
                )
                HintText("Base URL. We'll append /chat/completions if needed.")

                Spacer(modifier = Modifier.height(8.dp))

                TextInputField(
                    label = "Model",
                    value = state.customModel,
                    onValueChange = onCustomModelChanged,
                    placeholder = "claude-opus-4-5-20250416"
                )
                HintText("Exact model ID as expected by your endpoint.")

                Spacer(modifier = Modifier.height(8.dp))

                ApiKeyField(
                    label = "API Key",
                    value = state.customApiKey,
                    onValueChange = onCustomApiKeyChanged
                )
                HintText("Sent as 'Authorization: Bearer <key>'. Leave empty for local LLMs that don't require auth.")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Sensor API Keys ──
        SectionHeader("SENSOR API KEYS")
        HintText("These power the World Layer — location awareness and weather context. Without them, scoring still works but won't know WHERE you are. All keys are free.")

        Spacer(modifier = Modifier.height(12.dp))

        ApiKeyField("Google Places Key", state.placesKey, onPlacesKeyChanged)
        SetupGuide(
            title = "How to get a Google Places key (free)",
            steps = listOf(
                "1. Go to console.cloud.google.com",
                "2. Create a project (any name, e.g. \"Underscore\")",
                "3. Tap the hamburger menu > APIs & Services > Library",
                "4. Search \"Places API (New)\" and enable it",
                "5. Go to APIs & Services > Credentials",
                "6. Tap \"+ Create Credentials\" > \"API Key\"",
                "7. Copy the key and paste it above"
            ),
            note = "Free tier: \$200/month credit (covers ~40,000 lookups). You won't hit this."
        )

        Spacer(modifier = Modifier.height(16.dp))

        ApiKeyField("OpenWeatherMap Key", state.weatherKey, onWeatherKeyChanged)
        SetupGuide(
            title = "How to get an OpenWeatherMap key (free)",
            steps = listOf(
                "1. Go to openweathermap.org and sign up",
                "2. Check your email to verify your account",
                "3. Go to openweathermap.org/api_keys",
                "4. Your default key is already there — copy it",
                "5. Paste it above"
            ),
            note = "Free tier: 1,000 calls/day. Underscore uses ~1 per 10 minutes. Optional — adds rain/temperature context."
        )

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
            Switch(checked = state.batterySaver, onCheckedChange = onBatterySaverChanged)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Troubleshooting ──
        SectionHeader("TROUBLESHOOTING")
        Button(
            onClick = onShareDebugReport,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
        ) {
            Text(
                "REPORT BUG",
                letterSpacing = 2.sp,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = "Opens a GitHub issue with device info, config, and recent logs attached. One tap to submit.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 14.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

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
private fun HintText(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 14.sp,
        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
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
private fun TextInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = ""
) {
    var text by remember(value) { mutableStateOf(value) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onValueChange(it)
        },
        label = { Text(label, fontSize = 12.sp) },
        placeholder = {
            Text(
                placeholder,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
    )
}

@Composable
private fun SetupGuide(title: String, steps: List<String>, note: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { expanded = !expanded }
            .padding(10.dp)
    ) {
        Text(
            text = if (expanded) "▼ $title" else "▶ $title",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))
            steps.forEach { step ->
                Text(
                    text = step,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = note,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )
        }
    }
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
