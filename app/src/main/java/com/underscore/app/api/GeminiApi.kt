package com.underscore.app.api

import com.google.gson.Gson
import com.underscore.app.debug.AppLog
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

class GeminiApi(private val apiKey: String) : LlmProvider {

    override val name: String = "Gemini 2.5 Flash"

    companion object {
        private const val TAG = "GeminiApi"
        const val DEFAULT_API_KEY = "YOUR_GEMINI_API_KEY_HERE"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        private const val MODEL = "gemini-2.5-flash"
    }

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
        if (apiKey == DEFAULT_API_KEY) {
            AppLog.w(TAG, "generate() called with placeholder API key — skipping")
            return@withContext null
        }

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

            val url = "$BASE_URL/models/$MODEL:generateContent?key=$apiKey"
            AppLog.d(TAG, "Calling Gemini: $MODEL (prompt ${prompt.length} chars)")

            val httpRequest = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(httpRequest).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()?.take(500)
                AppLog.e(TAG, "Gemini API error ${response.code}: $errorBody")
                return@withContext null
            }

            val responseBody = response.body?.string()
            if (responseBody == null) {
                AppLog.e(TAG, "Gemini returned null body")
                return@withContext null
            }

            val geminiResponse = gson.fromJson(responseBody, GeminiResponse::class.java)
            val text = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (text == null) {
                AppLog.w(TAG, "Gemini returned no candidates/content. Response: ${responseBody.take(300)}")
            } else {
                AppLog.d(TAG, "Gemini response OK (${text.length} chars)")
            }

            text
        } catch (e: Exception) {
            AppLog.e(TAG, "Gemini request failed: ${e.message}", e)
            null
        }
    }
}
