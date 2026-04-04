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

    override val name: String = "Gemini 3 Flash"

    companion object {
        private const val TAG = "GeminiApi"
        const val DEFAULT_API_KEY = "YOUR_GEMINI_API_KEY_HERE"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        private const val PRIMARY_MODEL = "gemini-3-flash-preview"
        private const val FALLBACK_MODEL = "gemini-2.5-flash"
    }

    /** Call a specific model. Returns text on success, null on overload (503/429) so caller can fallback. */
    private fun callModel(model: String, body: okhttp3.RequestBody, promptLen: Int): String? {
        val url = "$BASE_URL/models/$model:generateContent?key=$apiKey"
        AppLog.d(TAG, "Calling Gemini: $model (prompt $promptLen chars)")

        val httpRequest = Request.Builder().url(url).post(body).build()
        val response = client.newCall(httpRequest).execute()

        if (response.code == 503 || response.code == 429) {
            val errorBody = response.body?.string()?.take(300)
            AppLog.w(TAG, "$model overloaded (${response.code}): $errorBody")
            return null
        }

        if (!response.isSuccessful) {
            val errorBody = response.body?.string()?.take(500)
            AppLog.e(TAG, "Gemini API error ${response.code}: $errorBody")
            return null
        }

        val responseBody = response.body?.string() ?: run {
            AppLog.e(TAG, "Gemini returned null body")
            return null
        }

        val geminiResponse = gson.fromJson(responseBody, GeminiResponse::class.java)
        val text = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

        if (text == null) {
            AppLog.w(TAG, "Gemini returned no candidates. Response: ${responseBody.take(300)}")
        } else {
            AppLog.d(TAG, "$model response OK (${text.length} chars)")
        }

        return text
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

            // Try primary model, fall back to 2.5-flash on overload (503/429)
            val result = callModel(PRIMARY_MODEL, body, prompt.length)
            if (result != null) return@withContext result

            AppLog.d(TAG, "Primary model unavailable, falling back to $FALLBACK_MODEL")
            callModel(FALLBACK_MODEL, body, prompt.length)
        } catch (e: Exception) {
            AppLog.e(TAG, "Gemini request failed: ${e.message}", e)
            null
        }
    }
}
