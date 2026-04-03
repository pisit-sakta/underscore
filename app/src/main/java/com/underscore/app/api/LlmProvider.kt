package com.underscore.app.api

interface LlmProvider {
    val name: String
    suspend fun generate(
        prompt: String,
        systemPrompt: String? = null,
        temperature: Float = 0.7f,
        maxTokens: Int = 2048,
        jsonMode: Boolean = false
    ): String?
}

enum class LlmProviderType {
    GEMINI,
    CLAUDE;

    val displayName: String get() = when (this) {
        GEMINI -> "Google Gemini 3 Flash"
        CLAUDE -> "Anthropic Claude Haiku"
    }

    val description: String get() = when (this) {
        GEMINI -> "Fastest. Cheapest. Deepest media knowledge."
        CLAUDE -> "Strong privacy stance. Excellent reasoning."
    }
}
