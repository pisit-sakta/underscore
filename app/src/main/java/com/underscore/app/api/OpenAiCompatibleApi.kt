package com.underscore.app.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

// OpenAI Chat Completion request/response (works with any compatible endpoint)
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float = 0.7f,
    @SerializedName("max_tokens") val maxTokens: Int = 2048,
    @SerializedName("response_format") val responseFormat: ResponseFormat? = null
)

data class ChatMessage(
    val role: String,   // "system", "user", "assistant"
    val content: String
)

data class ResponseFormat(
    val type: String    // "json_object" or "text"
)

data class ChatCompletionResponse(
    val choices: List<ChatChoice>?,
    val error: ChatError?
)

data class ChatChoice(
    val message: ChatMessage?,
    val index: Int = 0
)

data class ChatError(
    val message: String?,
    val type: String?
)

/**
 * OpenAI-compatible Chat Completion provider.
 *
 * Works with any endpoint that speaks the /v1/chat/completions format:
 * - OpenAI (api.openai.com)
 * - Anthropic via proxy (claude-code-proxy, litellm, etc.)
 * - OpenRouter (openrouter.ai)
 * - Local LLMs (ollama, llama.cpp, vLLM, text-generation-webui)
 * - TabbyAPI, koboldcpp, etc.
 * - Any custom proxy / gateway
 *
 * User provides:
 * - baseUrl: e.g. "https://my-proxy.railway.app/v1" or "http://localhost:8080/v1"
 * - model: e.g. "claude-opus-4-5-20250416", "gpt-4o", "llama-3-70b", whatever
 * - apiKey: sent as "Authorization: Bearer <key>" (can be empty for local LLMs)
 */
class OpenAiCompatibleApi(
    private val baseUrl: String,
    private val model: String,
    private val apiKey: String = ""
) : LlmProvider {

    companion object {
        private const val TAG = "OpenAiCompatibleApi"
    }

    override val name: String = "Custom ($model)"
    override val isConfigured: Boolean = baseUrl.isNotBlank() && model.isNotBlank()
    override var lastError: String? = null
        private set

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // longer timeout for slow endpoints / large models
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    override suspend fun generate(
        prompt: String,
        systemPrompt: String?,
        temperature: Float,
        maxTokens: Int,
        jsonMode: Boolean
    ): String? = withContext(Dispatchers.IO) {
        try {
            val messages = mutableListOf<ChatMessage>()

            if (systemPrompt != null) {
                messages.add(ChatMessage(role = "system", content = systemPrompt))
            }

            messages.add(ChatMessage(role = "user", content = prompt))

            val request = ChatCompletionRequest(
                model = model,
                messages = messages,
                temperature = temperature,
                maxTokens = maxTokens,
                responseFormat = if (jsonMode) ResponseFormat(type = "json_object") else null
            )

            val body = gson.toJson(request).toRequestBody(jsonMediaType)

            // Normalize URL: ensure it ends with /chat/completions
            val url = buildUrl()

            val httpRequestBuilder = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(body)

            // Add auth header if key is provided (some local LLMs don't need one)
            if (apiKey.isNotBlank()) {
                httpRequestBuilder.addHeader("Authorization", "Bearer $apiKey")
            }

            val httpRequest = httpRequestBuilder.build()

            Log.d(TAG, "Calling $url with model=$model")
            val response = client.newCall(httpRequest).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                lastError = "API ${response.code}: ${errorBody?.take(100)}"
                Log.e(TAG, "API error ${response.code}: $errorBody")
                return@withContext null
            }

            val responseBody = response.body?.string() ?: run {
                lastError = "Empty response body"
                return@withContext null
            }
            val chatResponse = gson.fromJson(responseBody, ChatCompletionResponse::class.java)

            if (chatResponse.error != null) {
                lastError = "API error: ${chatResponse.error.message}"
                Log.e(TAG, "API returned error: ${chatResponse.error.message}")
                return@withContext null
            }

            val content = chatResponse.choices?.firstOrNull()?.message?.content
            if (content == null) {
                lastError = "No content in response"
                Log.w(TAG, "No content in response")
            }
            content
        } catch (e: Exception) {
            lastError = "Network error: ${e.message}"
            Log.e(TAG, "Request failed", e)
            null
        }
    }

    private fun buildUrl(): String {
        var url = baseUrl.trimEnd('/')

        // User might provide:
        // "https://proxy.example.com/v1"                     -> append /chat/completions
        // "https://proxy.example.com/v1/"                    -> append chat/completions
        // "https://proxy.example.com/v1/chat/completions"    -> use as-is
        // "https://proxy.example.com"                        -> append /v1/chat/completions

        if (url.endsWith("/chat/completions")) {
            return url
        }

        if (url.endsWith("/v1")) {
            return "$url/chat/completions"
        }

        // If it doesn't end with /v1 or /chat/completions, try appending /v1/chat/completions
        // but only if there's no path beyond the host (avoid double-pathing)
        if (!url.contains("/v1")) {
            return "$url/v1/chat/completions"
        }

        // Fallback: just append /chat/completions
        return "$url/chat/completions"
    }
}
