package com.underscore.app.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class ClaudeRequest(
    val model: String,
    @SerializedName("max_tokens") val maxTokens: Int,
    val system: String? = null,
    val messages: List<ClaudeMessage>,
    val temperature: Float = 0.7f
)

data class ClaudeMessage(
    val role: String,
    val content: String
)

data class ClaudeResponse(
    val content: List<ClaudeContentBlock>?
)

data class ClaudeContentBlock(
    val type: String,
    val text: String?
)

class ClaudeApi(private val apiKey: String) : LlmProvider {

    companion object {
        const val DEFAULT_API_KEY = "YOUR_ANTHROPIC_API_KEY_HERE"
        private const val BASE_URL = "https://api.anthropic.com/v1/messages"
        private const val MODEL = "claude-haiku-4-5-20251001"
        private const val API_VERSION = "2023-06-01"
    }

    override val name: String = "Claude Haiku"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
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
            if (apiKey == DEFAULT_API_KEY) return@withContext null

            val messages = listOf(
                ClaudeMessage(role = "user", content = prompt)
            )

            val request = ClaudeRequest(
                model = MODEL,
                maxTokens = maxTokens,
                system = systemPrompt,
                messages = messages,
                temperature = temperature
            )

            val body = gson.toJson(request).toRequestBody(jsonMediaType)

            val httpRequest = Request.Builder()
                .url(BASE_URL)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", API_VERSION)
                .addHeader("content-type", "application/json")
                .post(body)
                .build()

            val response = client.newCall(httpRequest).execute()
            if (!response.isSuccessful) return@withContext null

            val responseBody = response.body?.string() ?: return@withContext null
            val claudeResponse = gson.fromJson(responseBody, ClaudeResponse::class.java)

            claudeResponse.content?.firstOrNull { it.type == "text" }?.text
        } catch (e: Exception) {
            null
        }
    }
}
