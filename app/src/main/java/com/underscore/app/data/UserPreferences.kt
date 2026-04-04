package com.underscore.app.data

import android.content.Context
import android.content.SharedPreferences
import com.underscore.app.api.LlmProviderType

class UserPreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "underscore_prefs"
        private const val KEY_LLM_PROVIDER = "llm_provider"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_CLAUDE_API_KEY = "claude_api_key"
        private const val KEY_CUSTOM_API_URL = "custom_api_url"
        private const val KEY_CUSTOM_API_KEY = "custom_api_key"
        private const val KEY_CUSTOM_MODEL = "custom_model"
        private const val KEY_WEATHER_API_KEY = "weather_api_key"
        private const val KEY_PLACES_API_KEY = "places_api_key"
        private const val KEY_BATTERY_SAVER = "battery_saver"
        private const val KEY_SPOTIFY_HINT_DISMISSED = "spotify_hint_dismissed"
        private const val KEY_SPOTIFY_SCOPE_VERSION = "spotify_scope_version"
        private const val KEY_DRAMA_SCALE = "drama_scale"
        private const val KEY_FOOD_ANALOGY_MODE = "food_analogy_mode"
        // Bump this when scopes change to force re-login
        const val CURRENT_SCOPE_VERSION = 2
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var llmProvider: LlmProviderType
        get() {
            val name = prefs.getString(KEY_LLM_PROVIDER, LlmProviderType.GEMINI.name)
            return try { LlmProviderType.valueOf(name!!) } catch (e: Exception) { LlmProviderType.GEMINI }
        }
        set(value) { prefs.edit().putString(KEY_LLM_PROVIDER, value.name).apply() }

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
        set(value) { prefs.edit().putString(KEY_GEMINI_API_KEY, value).apply() }

    var claudeApiKey: String
        get() = prefs.getString(KEY_CLAUDE_API_KEY, "") ?: ""
        set(value) { prefs.edit().putString(KEY_CLAUDE_API_KEY, value).apply() }

    // Custom OpenAI-compatible endpoint
    var customApiUrl: String
        get() = prefs.getString(KEY_CUSTOM_API_URL, "") ?: ""
        set(value) { prefs.edit().putString(KEY_CUSTOM_API_URL, value).apply() }

    var customApiKey: String
        get() = prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
        set(value) { prefs.edit().putString(KEY_CUSTOM_API_KEY, value).apply() }

    var customModel: String
        get() = prefs.getString(KEY_CUSTOM_MODEL, "") ?: ""
        set(value) { prefs.edit().putString(KEY_CUSTOM_MODEL, value).apply() }

    var weatherApiKey: String
        get() = prefs.getString(KEY_WEATHER_API_KEY, "") ?: ""
        set(value) { prefs.edit().putString(KEY_WEATHER_API_KEY, value).apply() }

    var placesApiKey: String
        get() = prefs.getString(KEY_PLACES_API_KEY, "") ?: ""
        set(value) { prefs.edit().putString(KEY_PLACES_API_KEY, value).apply() }

    var batterySaver: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_SAVER, false)
        set(value) { prefs.edit().putBoolean(KEY_BATTERY_SAVER, value).apply() }

    var spotifyHintDismissed: Boolean
        get() = prefs.getBoolean(KEY_SPOTIFY_HINT_DISMISSED, false)
        set(value) { prefs.edit().putBoolean(KEY_SPOTIFY_HINT_DISMISSED, value).apply() }

    var spotifyScopeVersion: Int
        get() = prefs.getInt(KEY_SPOTIFY_SCOPE_VERSION, 1)
        set(value) { prefs.edit().putInt(KEY_SPOTIFY_SCOPE_VERSION, value).apply() }

    var dramaScale: Int
        get() = prefs.getInt(KEY_DRAMA_SCALE, 5)
        set(value) { prefs.edit().putInt(KEY_DRAMA_SCALE, value.coerceIn(1, 10)).apply() }

    var foodAnalogyMode: Boolean
        get() = prefs.getBoolean(KEY_FOOD_ANALOGY_MODE, false)
        set(value) { prefs.edit().putBoolean(KEY_FOOD_ANALOGY_MODE, value).apply() }

    fun needsSpotifyRelogin(): Boolean = spotifyScopeVersion < CURRENT_SCOPE_VERSION

    fun markScopeVersionCurrent() {
        spotifyScopeVersion = CURRENT_SCOPE_VERSION
    }

    fun deleteAllData() {
        prefs.edit().clear().apply()
    }
}
