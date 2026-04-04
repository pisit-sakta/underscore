package com.underscore.app.narrative

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson

data class ProtagonistProfile(
    val name: String = "The Phantom Operative",
    val primaryGenres: List<String> = listOf("game_soundtracks", "cinematic_rock", "atmospheric_ambient"),
    val narrativeAesthetic: String = "Kojima-esque stealth action with philosophical undertones. Seinen manga energy.",
    val combatResponse: String = "Metal Gear Rising — escalation to survival/action",
    val contemplativeResponse: String = "MGSV ambient — existential but purposeful",
    val socialResponse: String = "Yakuza series — honorable, intense",
    val morningIdentity: String = "Nick Cave / Peaky Blinders — ominous readiness",
    val victoryResponse: String = "DMC5 / Bury the Light — triumph through endurance",
    val humorPreference: HumorPreference = HumorPreference.IRONIC_DRAMATIC,
    val emotionalProcessing: String = "internal — music as emotional exhaust"
)

enum class HumorPreference {
    IRONIC_DRAMATIC,   // Absurdly epic music for mundane situations
    SINCERE,           // Music matches the actual weight of the moment
    DEADPAN,           // Understated music for intense situations
    CHAOTIC            // Deliberately mismatched for comedic effect
}

class ProtagonistProfileManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "underscore_protagonist"
        private const val KEY_PROFILE = "active_profile"
        private const val KEY_CHARACTER = "character_name"

        // Hardcoded founder profile for MVP testing
        val FOUNDER_PROFILE = ProtagonistProfile(
            name = "The Phantom Operative",
            primaryGenres = listOf("game_soundtracks", "cinematic_rock", "atmospheric_ambient"),
            narrativeAesthetic = "Kojima-esque stealth action with philosophical undertones. Seinen manga energy.",
            combatResponse = "Metal Gear Rising — escalation to survival/action. 'The Only Thing I Know For Real.'",
            contemplativeResponse = "MGSV ambient — existential but purposeful. Liminal.",
            socialResponse = "Yakuza series — honorable, intense. 'Pledge of Demon.'",
            morningIdentity = "Nick Cave — 'Red Right Hand.' Peaky Blinders ominous readiness.",
            victoryResponse = "DMC5 — 'Bury the Light.' Triumph through endurance, not celebration.",
            humorPreference = HumorPreference.IRONIC_DRAMATIC,
            emotionalProcessing = "internal — music as emotional exhaust, not expression"
        )
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getActiveProfile(): ProtagonistProfile {
        val json = prefs.getString(KEY_PROFILE, null)
        return if (json != null) {
            try {
                gson.fromJson(json, ProtagonistProfile::class.java)
            } catch (e: Exception) {
                Log.e("ProtagonistProfile", "Failed to parse saved profile, using default: ${e.message}")
                FOUNDER_PROFILE
            }
        } else {
            FOUNDER_PROFILE
        }
    }

    fun saveProfile(profile: ProtagonistProfile) {
        prefs.edit().putString(KEY_PROFILE, gson.toJson(profile)).apply()
    }

    fun getActiveCharacterName(): String? = prefs.getString(KEY_CHARACTER, null)

    fun setActiveCharacter(name: String?) {
        if (name != null) {
            prefs.edit().putString(KEY_CHARACTER, name).apply()
        } else {
            prefs.edit().remove(KEY_CHARACTER).apply()
        }
    }

    fun buildPromptContext(): String {
        val profile = getActiveProfile()
        val character = getActiveCharacterName()

        return buildString {
            appendLine("PROTAGONIST PROFILE:")
            appendLine("Name: ${profile.name}")
            appendLine("Aesthetic: ${profile.narrativeAesthetic}")
            appendLine("Genres: ${profile.primaryGenres.joinToString(", ")}")
            appendLine("Combat response: ${profile.combatResponse}")
            appendLine("Contemplative: ${profile.contemplativeResponse}")
            appendLine("Social: ${profile.socialResponse}")
            appendLine("Morning: ${profile.morningIdentity}")
            appendLine("Victory: ${profile.victoryResponse}")
            appendLine("Humor: ${profile.humorPreference.name.lowercase()} — ${describeHumor(profile.humorPreference)}")
            appendLine("Emotional processing: ${profile.emotionalProcessing}")
            if (character != null) {
                appendLine("ACTIVE CHARACTER MODE: $character — constrain song selection to fit this character's franchise and emotional architecture")
            }
        }
    }

    private fun describeHumor(pref: HumorPreference): String = when (pref) {
        HumorPreference.IRONIC_DRAMATIC -> "play absurdly epic music for mundane situations (dramatic strings at taco stands, boss music choosing onigiri)"
        HumorPreference.SINCERE -> "match music weight to actual moment weight"
        HumorPreference.DEADPAN -> "understated music for intense situations"
        HumorPreference.CHAOTIC -> "deliberately mismatched music for comedic whiplash"
    }
}
