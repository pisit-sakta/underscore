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

// Gemini API request/response models
data class GeminiRequest(
    val contents: List<GeminiContent>,
    @SerializedName("generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String = "user"
)

data class GeminiPart(val text: String)

data class GeminiGenerationConfig(
    val temperature: Float = 0.7f,
    val maxOutputTokens: Int = 2048,
    @SerializedName("responseMimeType") val responseMimeType: String? = null
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?
)

class GeminiApi(private val apiKey: String) {

    companion object {
        // ⚠️ REPLACE THIS with your Google AI API key
        // Get one at https://aistudio.google.com/app/apikey
        const val DEFAULT_API_KEY = "YOUR_GEMINI_API_KEY_HERE"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        private const val MODEL = "gemini-3-flash"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    suspend fun generate(
        prompt: String,
        systemPrompt: String? = null,
        temperature: Float = 0.7f,
        maxTokens: Int = 2048,
        jsonMode: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        try {
            val contents = mutableListOf<GeminiContent>()

            if (systemPrompt != null) {
                contents.add(GeminiContent(
                    parts = listOf(GeminiPart(systemPrompt)),
                    role = "user"
                ))
                contents.add(GeminiContent(
                    parts = listOf(GeminiPart("Understood. I will follow these instructions.")),
                    role = "model"
                ))
            }

            contents.add(GeminiContent(
                parts = listOf(GeminiPart(prompt)),
                role = "user"
            ))

            val config = GeminiGenerationConfig(
                temperature = temperature,
                maxOutputTokens = maxTokens,
                responseMimeType = if (jsonMode) "application/json" else null
            )

            val request = GeminiRequest(contents = contents, generationConfig = config)
            val body = gson.toJson(request).toRequestBody(jsonMediaType)

            val httpRequest = Request.Builder()
                .url("$BASE_URL/models/$MODEL:generateContent?key=$apiKey")
                .post(body)
                .build()

            val response = client.newCall(httpRequest).execute()
            if (!response.isSuccessful) return@withContext null

            val responseBody = response.body?.string() ?: return@withContext null
            val geminiResponse = gson.fromJson(responseBody, GeminiResponse::class.java)

            geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        } catch (e: Exception) {
            null
        }
    }
}
