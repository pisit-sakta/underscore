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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.underscore.app.data.CharacterProfile

@Composable
fun CharacterSubScreen(
    characterModeEnabled: Boolean,
    activeCharacterName: String,
    characters: List<CharacterProfile>,
    isGeneratingCharacter: Boolean,
    characterError: String? = null,
    onCharacterModeChanged: (Boolean) -> Unit,
    onCharacterSelected: (String) -> Unit,
    onGenerateCharacter: (String) -> Unit,
    onBack: () -> Unit
) {
    var customName by remember { mutableStateOf("") }
    var localCharacterMode by remember { mutableStateOf(characterModeEnabled) }
    var localActiveCharacter by remember(activeCharacterName) { mutableStateOf(activeCharacterName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SubScreenHeader("CHARACTER", onBack)

        Spacer(modifier = Modifier.height(24.dp))

        // Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Character Mode", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (localCharacterMode && localActiveCharacter.isNotBlank()) localActiveCharacter
                    else "Score your life as a character",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = localCharacterMode, onCheckedChange = { localCharacterMode = it; onCharacterModeChanged(it) })
        }

        if (localCharacterMode) {
            Spacer(modifier = Modifier.height(16.dp))

            // Active character preview
            if (localActiveCharacter.isNotBlank()) {
                val activeChar = characters.find { it.name == localActiveCharacter }
                if (activeChar != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MiniDiagonalSplit(
                            color1 = parseHexColor(activeChar.color1),
                            color2 = parseHexColor(activeChar.color2),
                            modifier = Modifier
                                .height(24.dp)
                                .width(48.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
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
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Character list
            SectionHeader("SELECT CHARACTER")

            characters.forEach { character ->
                val isActive = character.name == localActiveCharacter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isActive) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        )
                        .clickable { localActiveCharacter = character.name; onCharacterSelected(character.name) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiniDiagonalSplit(
                        color1 = parseHexColor(character.color1),
                        color2 = parseHexColor(character.color2),
                        modifier = Modifier
                            .height(16.dp)
                            .width(16.dp)
                    )
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

            // ── Custom Character ──
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader("CREATE CUSTOM CHARACTER")

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
                enabled = customName.isNotBlank() && !isGeneratingCharacter,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
            ) {
                Text(
                    if (isGeneratingCharacter) "GENERATING..." else "GENERATE CHARACTER",
                    letterSpacing = 2.sp,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            if (isGeneratingCharacter) {
                Text(
                    "Gemini is building the profile + picking colors...",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (characterError != null && !isGeneratingCharacter) {
                Text(
                    characterError,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

