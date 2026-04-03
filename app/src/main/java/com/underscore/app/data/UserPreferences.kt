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
        private const val KEY_WEATHER_API_KEY = "weather_api_key"
        private const val KEY_PLACES_API_KEY = "places_api_key"
        private const val KEY_BATTERY_SAVER = "battery_saver"
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

    var weatherApiKey: String
        get() = prefs.getString(KEY_WEATHER_API_KEY, "") ?: ""
        set(value) { prefs.edit().putString(KEY_WEATHER_API_KEY, value).apply() }

    var placesApiKey: String
        get() = prefs.getString(KEY_PLACES_API_KEY, "") ?: ""
        set(value) { prefs.edit().putString(KEY_PLACES_API_KEY, value).apply() }

    var batterySaver: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_SAVER, false)
        set(value) { prefs.edit().putBoolean(KEY_BATTERY_SAVER, value).apply() }

    fun deleteAllData() {
        prefs.edit().clear().apply()
    }
}
