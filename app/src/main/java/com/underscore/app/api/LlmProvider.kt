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
    CLAUDE,
    OPENAI_COMPATIBLE;

    val displayName: String get() = when (this) {
        GEMINI -> "Google Gemini 2.5 Flash"
        CLAUDE -> "Anthropic Claude"
        OPENAI_COMPATIBLE -> "Custom (OpenAI-compatible)"
    }

    val description: String get() = when (this) {
        GEMINI -> "Fastest. Cheapest. Deepest media knowledge. Uses Google AI API."
        CLAUDE -> "Strong privacy stance. Excellent reasoning. Uses Anthropic Messages API."
        OPENAI_COMPATIBLE -> "Any OpenAI-compatible endpoint: OpenRouter, local LLMs, proxy servers, self-hosted. You provide the URL + model + key."
    }
}
