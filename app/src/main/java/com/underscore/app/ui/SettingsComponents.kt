package com.underscore.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.underscore.app.api.KeyCheckResult
import com.underscore.app.api.KeyValidator
import com.underscore.app.api.LlmProviderType
import com.underscore.app.data.CharacterProfile
import kotlinx.coroutines.launch

/** Shared state for settings sub-screens. */
data class SettingsState(
    val provider: LlmProviderType = LlmProviderType.GEMINI,
    val geminiKey: String = "",
    val claudeKey: String = "",
    val customApiUrl: String = "",
    val customApiKey: String = "",
    val customModel: String = "",
    val proxyPassword: String = "",
    val weatherKey: String = "",

    val batterySaver: Boolean = false,
    val dramaScale: Int = 5,
    val foodAnalogyMode: Boolean = false,
    val customMood: String = "",
    val moodExpiresAt: Long = 0L,
    val characterModeEnabled: Boolean = false,
    val activeCharacterName: String = "",
    val characters: List<CharacterProfile> = emptyList(),
    val isGeneratingCharacter: Boolean = false
)

/** Reusable sub-screen header with back button and title. */
@Composable
fun SubScreenHeader(title: String, onBack: () -> Unit) {
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
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = 4.sp
        )
    }
}

@Composable
fun SectionHeader(title: String) {
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
fun HintText(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 14.sp,
        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
    )
}

@Composable
fun ProviderOption(
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
fun ApiKeyField(label: String, value: String, onValueChange: (String) -> Unit) {
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
fun ApiKeyFieldWithCheck(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onCheck: suspend (String) -> KeyCheckResult
) {
    val scope = rememberCoroutineScope()
    var checkState by remember { mutableStateOf<CheckState>(CheckState.Idle) }
    var currentKey by remember(value) { mutableStateOf(value) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = currentKey,
                onValueChange = {
                    currentKey = it
                    onValueChange(it)
                    checkState = CheckState.Idle
                },
                label = { Text(label, fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (currentKey.isNotBlank()) {
                        checkState = CheckState.Checking
                        scope.launch {
                            checkState = try {
                                when (val result = onCheck(currentKey)) {
                                    is KeyCheckResult.Valid -> CheckState.Valid
                                    is KeyCheckResult.Invalid -> CheckState.Failed(result.reason)
                                    is KeyCheckResult.Error -> CheckState.Failed(result.message)
                                }
                            } catch (e: Exception) {
                                CheckState.Failed(e.message ?: "Check failed")
                            }
                        }
                    }
                },
                enabled = currentKey.isNotBlank() && checkState != CheckState.Checking,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (checkState == CheckState.Checking) {
                    Text("...", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                } else {
                    Text("CHECK", fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
                }
            }
        }
        when (val state = checkState) {
            is CheckState.Valid -> Text(
                "Valid",
                fontSize = 11.sp,
                color = Color(0xFF16A34A),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
            is CheckState.Failed -> Text(
                state.reason,
                fontSize = 11.sp,
                color = Color(0xFFDC2626),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
            else -> {}
        }
    }
}

private sealed class CheckState {
    object Idle : CheckState()
    object Checking : CheckState()
    object Valid : CheckState()
    data class Failed(val reason: String) : CheckState()
}

@Composable
fun EndpointCheckButton(url: String, model: String, apiKey: String) {
    val scope = rememberCoroutineScope()
    var checkState by remember { mutableStateOf<CheckState>(CheckState.Idle) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = {
                checkState = CheckState.Checking
                scope.launch {
                    checkState = try {
                        when (val result = KeyValidator.checkOpenAiCompatible(url, model, apiKey)) {
                            is KeyCheckResult.Valid -> CheckState.Valid
                            is KeyCheckResult.Invalid -> CheckState.Failed(result.reason)
                            is KeyCheckResult.Error -> CheckState.Failed(result.message)
                        }
                    } catch (e: Exception) {
                        CheckState.Failed(e.message ?: "Check failed")
                    }
                }
            },
            enabled = url.isNotBlank() && model.isNotBlank() && checkState != CheckState.Checking,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                if (checkState == CheckState.Checking) "TESTING..." else "TEST CONNECTION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
        }
        when (val state = checkState) {
            is CheckState.Valid -> Text(
                "Connected",
                fontSize = 11.sp,
                color = Color(0xFF16A34A),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 12.dp)
            )
            is CheckState.Failed -> Text(
                state.reason,
                fontSize = 11.sp,
                color = Color(0xFFDC2626),
                modifier = Modifier.padding(start = 12.dp)
            )
            else -> {}
        }
    }
}

@Composable
fun TextInputField(
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
fun SetupGuide(title: String, steps: List<String>, note: String) {
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
