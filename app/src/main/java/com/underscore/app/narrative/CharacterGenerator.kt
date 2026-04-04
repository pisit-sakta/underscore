package com.underscore.app.narrative

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.underscore.app.api.LlmProvider
import com.underscore.app.data.CharacterProfile
import com.underscore.app.debug.AppLog

data class GeneratedCharacter(
    val name: String,
    val franchise: String,
    val tagline: String,
    val color1: String,
    val color2: String,
    @SerializedName("color_reference") val colorReference: String,
    @SerializedName("narrative_aesthetic") val narrativeAesthetic: String,
    @SerializedName("primary_genres") val primaryGenres: List<String>,
    @SerializedName("transition_style") val transitionStyle: String,
    @SerializedName("emotional_architecture") val emotionalArchitecture: Map<String, String>,
    @SerializedName("humor_preference") val humorPreference: String
)

class CharacterGenerator(private val llmProvider: LlmProvider) {

    companion object {
        private const val TAG = "CharacterGenerator"

        private val SYSTEM_PROMPT = """
You are a character profile generator for Underscore, an app that scores real life with music.
Given a character name, generate a complete character profile for the music scoring engine.

You MUST return valid JSON with these exact fields:
- name: The character's full name as recognized by fans
- franchise: The source material (anime, show, game, etc.)
- tagline: An iconic one-liner associated with this character (max 50 chars)
- color1: A hex color code for the character's PRIMARY visual identity
- color2: A hex color code for the character's SECONDARY visual identity
- color_reference: Why these two colors represent this character
- narrative_aesthetic: 2-3 sentences describing how this character's life should be scored
- primary_genres: Array of 3-5 music genres that fit this character
- transition_style: One of: "smooth", "jarring", "vinyl_crackle"
- emotional_architecture: Object with keys: morning, commute, work, social, confrontation, evening, setback, victory. Each value is a 1-2 sentence description of what music fits this character in that situation.
- humor_preference: One of: "ironic_dramatic", "sincere", "deadpan", "chaotic"

COLOR RULES (CRITICAL):
Select two colors from this character's visual identity that:
1. LOOK BEAUTIFUL together on a diagonal split phone screen
2. Are immediately recognizable to fans of the franchise
3. Use MUTED/DESATURATED tones, not raw saturated primaries
   (burnt orange not traffic-cone orange; midnight blue not crayon blue)
4. Create sufficient contrast to read as TWO DISTINCT fields
5. Do NOT resemble any national flag, sports team, corporate logo, or fast food brand

General rule: one DARK + one ACCENT. Dark anchors, accent provides character identity.
Bad: bright green + bright yellow (Brazil), red + green (Christmas), red + yellow (McDonald's)
Good: deep forest green + matte black, warm crimson + bone gold, cool white + electric blue

Return ONLY the JSON object, no markdown fences, no explanation.
""".trimIndent()
    }

    private val gson = Gson()

    data class CharacterResult(
        val profile: CharacterProfile? = null,
        val error: String? = null
    )

    suspend fun generate(characterName: String): CharacterResult {
        AppLog.d(TAG, "Generating profile for: $characterName")

        if (!llmProvider.isConfigured) {
            val msg = "LLM provider not configured (${llmProvider.name}). Add your API key in Settings."
            AppLog.e(TAG, msg)
            return CharacterResult(error = msg)
        }

        val prompt = "Generate a character profile for: $characterName"

        val response = llmProvider.generate(
            prompt = prompt,
            systemPrompt = SYSTEM_PROMPT,
            temperature = 0.7f,
            maxTokens = 2048,
            jsonMode = true
        )

        if (response == null) {
            val detail = llmProvider.lastError ?: "unknown error"
            AppLog.e(TAG, "LLM returned null for character '$characterName': $detail")
            return CharacterResult(error = "LLM call failed: $detail")
        }

        AppLog.d(TAG, "LLM response for character '$characterName': ${response.take(200)}")

        return try {
            // Strip markdown fences if present
            var cleaned = response.trim()
            if (cleaned.startsWith("```")) {
                val firstNewline = cleaned.indexOf('\n')
                if (firstNewline > 0) cleaned = cleaned.substring(firstNewline + 1)
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length - 3).trim()
            }

            val generated = gson.fromJson(cleaned, GeneratedCharacter::class.java)

            val profile = CharacterProfile(
                name = generated.name,
                franchise = generated.franchise,
                tagline = generated.tagline,
                color1 = generated.color1,
                color2 = generated.color2,
                colorReference = generated.colorReference,
                narrativeAesthetic = generated.narrativeAesthetic,
                primaryGenres = gson.toJson(generated.primaryGenres),
                transitionStyle = generated.transitionStyle,
                emotionalArchitecture = gson.toJson(generated.emotionalArchitecture),
                humorPreference = generated.humorPreference,
                isPreset = false
            )
            CharacterResult(profile = profile)
        } catch (e: Exception) {
            AppLog.e(TAG, "Failed to parse generated character: ${e.message}", e)
            CharacterResult(error = "Parse failed: ${e.message}")
        }
    }

    private val FRANCHISE_PROMPT = """
You are a franchise profile generator for Underscore, an app that scores real life with music.
Given a free-text input describing a franchise (and optionally a mood/tone/arc), generate a profile
for the franchise's overall musical identity.

The input may be just a franchise name ("Spy x Family") or a franchise + mood/tone
("nostalgic Yakuza", "dark Re:Zero", "JJK Hidden Inventory slice of life").

Parse the input into:
- name: The franchise name as recognized by fans
- mood: The mood/tone modifier, or empty string if none specified

Then generate:
- color1: Hex color for the franchise's PRIMARY visual identity
- color2: Hex color for the franchise's SECONDARY visual identity
- color_reference: Why these colors represent this franchise
- aesthetic: 2-3 sentences describing the franchise's OVERALL musical identity and how the mood
  modifier (if any) shifts the soundtrack selection. This is NOT about one character — it's about
  the full musical palette of the franchise.
- primary_genres: Array of 3-5 music genres that define this franchise's soundtrack

COLOR RULES: Same as character profiles — muted/desaturated tones, one dark + one accent,
no flag/brand lookalikes. Colors represent the FRANCHISE, not a specific character.

Return ONLY valid JSON, no markdown fences.
""".trimIndent()

    data class FranchiseResult(
        val profile: com.underscore.app.data.FranchiseProfile? = null,
        val error: String? = null
    )

    suspend fun generateFranchise(input: String): FranchiseResult {
        AppLog.d(TAG, "Generating franchise profile for: $input")

        if (!llmProvider.isConfigured) {
            val msg = "LLM provider not configured (${llmProvider.name}). Add your API key in Settings."
            AppLog.e(TAG, msg)
            return FranchiseResult(error = msg)
        }

        val prompt = "Generate a franchise profile for: $input"

        val response = llmProvider.generate(
            prompt = prompt,
            systemPrompt = FRANCHISE_PROMPT,
            temperature = 0.7f,
            maxTokens = 1024,
            jsonMode = true
        )

        if (response == null) {
            val detail = llmProvider.lastError ?: "unknown error"
            AppLog.e(TAG, "LLM returned null for franchise '$input': $detail")
            return FranchiseResult(error = "LLM call failed: $detail")
        }

        AppLog.d(TAG, "LLM response for franchise '$input': ${response.take(200)}")

        return try {
            // Strip markdown fences if present
            var cleaned = response.trim()
            if (cleaned.startsWith("```")) {
                val firstNewline = cleaned.indexOf('\n')
                if (firstNewline > 0) cleaned = cleaned.substring(firstNewline + 1)
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length - 3).trim()
            }

            val profile = gson.fromJson(cleaned, com.underscore.app.data.FranchiseProfile::class.java)
            if (profile == null || profile.name.isBlank()) {
                FranchiseResult(error = "LLM returned invalid profile (empty name)")
            } else {
                FranchiseResult(profile = profile)
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "Failed to parse franchise profile: ${e.message}", e)
            FranchiseResult(error = "Parse failed: ${e.message}")
        }
    }
}
