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
import com.underscore.app.data.FranchiseProfile

@Composable
fun FranchiseSubScreen(
    franchiseEnabled: Boolean,
    activeFranchise: FranchiseProfile?,
    isGenerating: Boolean,
    onToggle: (Boolean) -> Unit,
    onGenerate: (String) -> Unit,
    onBack: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var localEnabled by remember { mutableStateOf(franchiseEnabled) }
    // Sync with parent state when franchise profile is generated async
    var localFranchise by remember(activeFranchise) { mutableStateOf(activeFranchise) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SubScreenHeader("FRANCHISE IMMERSION", onBack)

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
                Text("Franchise Immersion", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (localEnabled && localFranchise != null) {
                        val mood = localFranchise!!.mood
                        if (mood.isNotBlank()) "${localFranchise!!.name} ($mood)" else localFranchise!!.name
                    } else "Immerse in a franchise's soundtrack",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = localEnabled, onCheckedChange = { localEnabled = it; onToggle(it) })
        }

        if (localEnabled) {
            Spacer(modifier = Modifier.height(16.dp))

            // Active franchise display
            if (localFranchise != null) {
                val franchise = localFranchise!!
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MiniDiagonalSplit(
                            color1 = parseHexColor(franchise.color1),
                            color2 = parseHexColor(franchise.color2),
                            modifier = Modifier
                                .height(24.dp)
                                .width(48.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                franchise.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (franchise.mood.isNotBlank()) {
                                Text(
                                    franchise.mood,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        franchise.aesthetic,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Input
            SectionHeader("SET FRANCHISE")
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Franchise + mood", fontSize = 12.sp) },
                placeholder = {
                    Text(
                        "Spy x Family, nostalgic Yakuza, dark Re:Zero, JJK Hidden Inventory slice of life...",
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
                    if (inputText.isNotBlank()) {
                        onGenerate(inputText)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = inputText.isNotBlank() && !isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
            ) {
                Text(
                    if (isGenerating) "GENERATING..." else "SET FRANCHISE",
                    letterSpacing = 2.sp,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            if (isGenerating) {
                Text(
                    "Building franchise profile + picking colors...",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
