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
    blendModeEnabled: Boolean,
    blendMorning: String,
    blendAfternoon: String,
    blendEvening: String,
    blendNight: String,
    onCharacterModeChanged: (Boolean) -> Unit,
    onCharacterSelected: (String) -> Unit,
    onGenerateCharacter: (String) -> Unit,
    onBlendModeChanged: (Boolean) -> Unit,
    onBlendSlotChanged: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var customName by remember { mutableStateOf("") }

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
                    if (characterModeEnabled && activeCharacterName.isNotBlank()) activeCharacterName
                    else "Score your life as a character",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = characterModeEnabled, onCheckedChange = onCharacterModeChanged)
        }

        if (characterModeEnabled) {
            Spacer(modifier = Modifier.height(16.dp))

            // Active character preview
            if (activeCharacterName.isNotBlank()) {
                val activeChar = characters.find { it.name == activeCharacterName }
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

            // ── Blend Mode ──
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Blend Mode", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Different characters for each time of day",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = blendModeEnabled, onCheckedChange = onBlendModeChanged)
            }

            if (blendModeEnabled && characters.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val slots = listOf(
                    Triple("MORNING", "06:00 – 12:00", blendMorning),
                    Triple("AFTERNOON", "12:00 – 17:00", blendAfternoon),
                    Triple("EVENING", "17:00 – 21:00", blendEvening),
                    Triple("NIGHT", "21:00 – 06:00", blendNight)
                )
                slots.forEach { (slot, timeRange, currentChar) ->
                    BlendSlotRow(
                        slot = slot,
                        timeRange = timeRange,
                        currentChar = currentChar,
                        characters = characters,
                        onCharacterSelected = { name -> onBlendSlotChanged(slot, name) }
                    )
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
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun BlendSlotRow(
    slot: String,
    timeRange: String,
    currentChar: String,
    characters: List<CharacterProfile>,
    onCharacterSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayName = currentChar.ifBlank { "Not set" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (currentChar.isNotBlank()) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else Color.Transparent
                )
                .clickable { expanded = !expanded }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(slot, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                Text(timeRange, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentChar.isNotBlank()) {
                    val char = characters.find { it.name == currentChar }
                    if (char != null) {
                        MiniDiagonalSplit(
                            color1 = parseHexColor(char.color1),
                            color2 = parseHexColor(char.color2),
                            modifier = Modifier.height(12.dp).width(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
                Text(
                    displayName,
                    fontSize = 11.sp,
                    color = if (currentChar.isNotBlank()) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (currentChar.isNotBlank()) FontWeight.Medium else FontWeight.Normal
                )
                Text(if (expanded) " ▼" else " ▶", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
            }
        }

        if (expanded) {
            characters.forEach { char ->
                Text(
                    text = char.name,
                    fontSize = 11.sp,
                    color = if (char.name == currentChar) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (char.name == currentChar) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCharacterSelected(char.name); expanded = false }
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}
