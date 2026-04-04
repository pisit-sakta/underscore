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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.underscore.app.data.DramaScale

data class SettingsState(
    val provider: LlmProviderType = LlmProviderType.GEMINI,
    val geminiKey: String = "",
    val claudeKey: String = "",
    val customApiUrl: String = "",
    val customApiKey: String = "",
    val customModel: String = "",
    val weatherKey: String = "",
    val placesKey: String = "",
    val batterySaver: Boolean = false,
    val dramaScale: Int = 5,
    val foodAnalogyMode: Boolean = false,
    val customMood: String = "",
    val moodExpiresAt: Long = 0L,
    val characterModeEnabled: Boolean = false,
    val activeCharacterName: String = "",
    val characters: List<com.underscore.app.data.CharacterProfile> = emptyList(),
    val isGeneratingCharacter: Boolean = false
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
    onDramaScaleChanged: (Int) -> Unit,
    onFoodAnalogyChanged: (Boolean) -> Unit,
    onMoodChanged: (String, Long) -> Unit,
    onMoodCleared: () -> Unit,
    onCharacterModeChanged: (Boolean) -> Unit,
    onCharacterSelected: (String) -> Unit,
    onGenerateCharacter: (String) -> Unit,
    onDeleteAllData: () -> Unit,
    onShareDebugReport: () -> Unit,
    onBack: () -> Unit
) {
    // Local mutable state so UI updates immediately on change
    var selectedProvider by remember { mutableStateOf(state.provider) }
    var savedToast by remember { mutableStateOf(false) }

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

        // ── Character Mode ──
        CharacterModeSection(
            enabled = state.characterModeEnabled,
            activeCharacterName = state.activeCharacterName,
            characters = state.characters,
            isGenerating = state.isGeneratingCharacter,
            onToggle = onCharacterModeChanged,
            onCharacterSelected = onCharacterSelected,
            onGenerateCharacter = onGenerateCharacter
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Drama Scale ──
        DramaScaleSection(
            dramaScale = state.dramaScale,
            foodMode = state.foodAnalogyMode,
            onDramaScaleChanged = onDramaScaleChanged,
            onFoodModeChanged = onFoodAnalogyChanged
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Vibe Check (Custom Mood) ──
        VibeCheckSection(
            currentMood = state.customMood,
            moodExpiresAt = state.moodExpiresAt,
            onMoodSet = onMoodChanged,
            onMoodCleared = onMoodCleared
        )

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

        Spacer(modifier = Modifier.height(20.dp))

        // ── Save Button ──
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
private fun CharacterModeSection(
    enabled: Boolean,
    activeCharacterName: String,
    characters: List<com.underscore.app.data.CharacterProfile>,
    isGenerating: Boolean,
    onToggle: (Boolean) -> Unit,
    onCharacterSelected: (String) -> Unit,
    onGenerateCharacter: (String) -> Unit
) {
    var customName by remember { mutableStateOf("") }

    SectionHeader("CHARACTER MODE")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        // Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Character Mode", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (enabled && activeCharacterName.isNotBlank()) activeCharacterName
                    else "Score your life as a character",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }

        if (enabled) {
            Spacer(modifier = Modifier.height(12.dp))

            // Active character color preview
            if (activeCharacterName.isNotBlank()) {
                val activeChar = characters.find { it.name == activeCharacterName }
                if (activeChar != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Color preview boxes
                        androidx.compose.foundation.Canvas(
                            modifier = Modifier
                                .height(24.dp)
                                .width(48.dp)
                                .clip(RoundedCornerShape(4.dp))
                        ) {
                            val w = size.width
                            val h = size.height
                            val path1 = androidx.compose.ui.graphics.Path().apply {
                                moveTo(0f, 0f); lineTo(w, 0f); lineTo(0f, h); close()
                            }
                            val path2 = androidx.compose.ui.graphics.Path().apply {
                                moveTo(w, 0f); lineTo(w, h); lineTo(0f, h); close()
                            }
                            drawPath(path1, parseHexColor(activeChar.color1))
                            drawPath(path2, parseHexColor(activeChar.color2))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                activeChar.tagline,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                activeChar.franchise,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Character list
            Text(
                "SELECT CHARACTER",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            characters.forEach { character ->
                val isActive = character.name == activeCharacterName
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isActive) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        )
                        .clickable { onCharacterSelected(character.name) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mini color preview
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .height(16.dp)
                            .width(16.dp)
                            .clip(RoundedCornerShape(2.dp))
                    ) {
                        drawRect(parseHexColor(character.color1))
                        val halfPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width, 0f)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(halfPath, parseHexColor(character.color2))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            character.name,
                            fontSize = 13.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            character.franchise,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!character.isPreset) {
                        Text(
                            "CUSTOM",
                            fontSize = 9.sp,
                            color = Color(0xFF7C3AED),
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Custom character generation
            Text(
                "CREATE CUSTOM CHARACTER",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = customName,
                onValueChange = { customName = it },
                label = { Text("Character name", fontSize = 12.sp) },
                placeholder = {
                    Text(
                        "Reinhard van Astrea, Makima, Levi Ackerman...",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (customName.isNotBlank()) {
                        onGenerateCharacter(customName)
                        customName = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = customName.isNotBlank() && !isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
            ) {
                Text(
                    if (isGenerating) "GENERATING..." else "GENERATE CHARACTER",
                    letterSpacing = 2.sp,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            if (isGenerating) {
                Text(
                    "Gemini is building the profile + picking colors...",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

private enum class MoodDuration(val label: String, val ms: Long) {
    UNTIL_CLEARED("Until I change it", 0L),
    TWO_HOURS("Next 2 hours", 2 * 60 * 60 * 1000L),
    TODAY("Today", 12 * 60 * 60 * 1000L)  // ~12 hours as approximation
}

private val MOOD_PRESETS = listOf(
    "Confident", "Melancholic", "Motivated", "Rainy Day",
    "Late Night", "Villain Arc", "Main Character", "Vibing"
)

@Composable
private fun VibeCheckSection(
    currentMood: String,
    moodExpiresAt: Long,
    onMoodSet: (String, Long) -> Unit,
    onMoodCleared: () -> Unit
) {
    var moodText by remember(currentMood) { mutableStateOf(currentMood) }
    var selectedDuration by remember { mutableStateOf(MoodDuration.UNTIL_CLEARED) }
    val hasMood = currentMood.isNotBlank()

    SectionHeader("VIBE CHECK")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        if (hasMood) {
            // Active mood display
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
                onClick = {
                    moodText = ""
                    onMoodCleared()
                },
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

        Spacer(modifier = Modifier.height(8.dp))

        // Quick presets
        Text(
            text = "QUICK VIBES",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Two rows of 4 presets
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

        Spacer(modifier = Modifier.height(8.dp))

        // Duration selector
        Text(
            text = "DURATION",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
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

        Spacer(modifier = Modifier.height(12.dp))

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
            Text(
                "SET VIBE",
                letterSpacing = 2.sp,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DramaScaleSection(
    dramaScale: Int,
    foodMode: Boolean,
    onDramaScaleChanged: (Int) -> Unit,
    onFoodModeChanged: (Boolean) -> Unit
) {
    var sliderValue by remember(dramaScale) { mutableStateOf(dramaScale.toFloat()) }
    val currentLevel = DramaScale.getLevel(sliderValue.toInt())
    var expanded by remember { mutableStateOf(false) }

    SectionHeader("DRAMA SCALE")

    // Current level display
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (foodMode) currentLevel.foodName else currentLevel.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${sliderValue.toInt()} / 10",
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (foodMode) currentLevel.foodOneLiner else currentLevel.oneLiner,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Slider
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onDramaScaleChanged(sliderValue.toInt()) },
            valueRange = 1f..10f,
            steps = 8,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )

        // Food analogy toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Food Analogy Mode",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Switch(checked = foodMode, onCheckedChange = onFoodModeChanged)
        }

        // Expandable: all levels preview
        if (expanded) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "ALL LEVELS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            DramaScale.levels.forEach { level ->
                val name = if (foodMode) level.foodName else level.name
                val desc = if (foodMode) level.foodOneLiner else level.oneLiner
                val isActive = level.level == sliderValue.toInt()
                Text(
                    text = "${level.level}. $name",
                    fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }

        // Expand hint
        Text(
            text = if (expanded) "▼ Tap to collapse" else "▶ Tap to see all levels",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp)
        )
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
            "Airplane mode works. Turn off network and verify. Or try intercepting network traffic. We dare you.",
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
