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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.underscore.app.api.KeyValidator
import com.underscore.app.api.LlmProviderType

@Composable
fun SettingsSubScreen(
    state: SettingsState,
    onProviderChanged: (LlmProviderType) -> Unit,
    onGeminiKeyChanged: (String) -> Unit,
    onClaudeKeyChanged: (String) -> Unit,
    onCustomApiUrlChanged: (String) -> Unit,
    onCustomApiKeyChanged: (String) -> Unit,
    onCustomModelChanged: (String) -> Unit,
    onProxyPasswordChanged: (String) -> Unit,
    onWeatherKeyChanged: (String) -> Unit,

    onBatterySaverChanged: (Boolean) -> Unit,
    onDeleteAllData: () -> Unit,
    onShareDebugReport: () -> Unit,
    onBack: () -> Unit
) {
    var selectedProvider by remember { mutableStateOf(state.provider) }
    var savedToast by remember { mutableStateOf(false) }
    var localBatterySaver by remember { mutableStateOf(state.batterySaver) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SubScreenHeader("SETTINGS", onBack)

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
                isSelected = selectedProvider == provider,
                onSelect = {
                    selectedProvider = provider
                    onProviderChanged(provider)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Provider-specific config ──
        when (selectedProvider) {
            LlmProviderType.GEMINI -> {
                SectionHeader("GEMINI CONFIG")
                ApiKeyFieldWithCheck(
                    label = "Google AI API Key",
                    value = state.geminiKey,
                    onValueChange = onGeminiKeyChanged,
                    onCheck = { KeyValidator.checkGemini(it) }
                )
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
                ApiKeyFieldWithCheck(
                    label = "Anthropic API Key",
                    value = state.claudeKey,
                    onValueChange = onClaudeKeyChanged,
                    onCheck = { KeyValidator.checkClaude(it) }
                )
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
                HintText("Any OpenAI-compatible API: OpenRouter, local LLMs, proxy servers, self-hosted models.")

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

                Spacer(modifier = Modifier.height(8.dp))

                ApiKeyField(
                    label = "API Key",
                    value = state.customApiKey,
                    onValueChange = onCustomApiKeyChanged
                )
                HintText("Leave empty for local LLMs that don't require auth.")

                Spacer(modifier = Modifier.height(8.dp))

                ApiKeyField(
                    label = "Proxy Password",
                    value = state.proxyPassword,
                    onValueChange = onProxyPasswordChanged
                )
                HintText("Optional. If set, sent as Bearer token instead of API key. For reverse proxies.")

                Spacer(modifier = Modifier.height(8.dp))

                EndpointCheckButton(
                    url = state.customApiUrl,
                    model = state.customModel,
                    apiKey = if (state.proxyPassword.isNotBlank()) state.proxyPassword else state.customApiKey
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Weather ──
        SectionHeader("WEATHER & LOCATION")
        HintText("Location uses free OpenStreetMap data. No key needed.")

        Spacer(modifier = Modifier.height(12.dp))

        ApiKeyFieldWithCheck(
            label = "OpenWeatherMap Key",
            value = state.weatherKey,
            onValueChange = onWeatherKeyChanged,
            onCheck = { KeyValidator.checkWeather(it) }
        )
        SetupGuide(
            title = "How to get an OpenWeatherMap key (free)",
            steps = listOf(
                "1. Go to openweathermap.org and sign up",
                "2. Verify your email",
                "3. Go to openweathermap.org/api_keys",
                "4. Copy your default key",
                "5. Paste above"
            ),
            note = "Free tier: 1,000 calls/day. Optional — adds weather context."
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Save ──
        Button(
            onClick = { savedToast = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
        ) {
            Text(
                if (savedToast) "SAVED" else "SAVE SETTINGS",
                letterSpacing = 2.sp,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        if (savedToast) {
            Text(
                text = "All settings are saved automatically. This button is for your peace of mind.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

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
            Switch(checked = localBatterySaver, onCheckedChange = { localBatterySaver = it; onBatterySaverChanged(it) })
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Troubleshooting ──
        SectionHeader("TROUBLESHOOTING")
        Button(
            onClick = onShareDebugReport,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
        ) {
            Text("REPORT BUG", letterSpacing = 2.sp, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Privacy ──
        SectionHeader("PRIVACY")
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
            listOf(
                "No data goes to our servers. We don't have data servers.",
                "Location, heart rate, movement — stays on YOUR device.",
                "The LLM receives abstracted scene data only. No GPS. No user ID.",
                "You choose your LLM provider. You control the trust tradeoff.",
                "Airplane mode works. Turn off network and verify.",
                "One-button data deletion. Delete All Data below. Gone."
            ).forEach { item ->
                Text(
                    text = "- $item",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onDeleteAllData,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B))
        ) {
            Text("DELETE ALL DATA", letterSpacing = 2.sp, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
