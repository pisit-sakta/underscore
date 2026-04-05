package com.underscore.app.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

sealed class KeyCheckResult {
    object Valid : KeyCheckResult()
    data class Invalid(val reason: String) : KeyCheckResult()
    data class Error(val message: String) : KeyCheckResult()
}

object KeyValidator {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = "application/json".toMediaType()

    suspend fun checkGemini(apiKey: String): KeyCheckResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext KeyCheckResult.Invalid("No key entered")
        try {
            val body = """{"contents":[{"parts":[{"text":"hi"}]}],"generationConfig":{"maxOutputTokens":1}}"""
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder().url(url).post(body.toRequestBody(json)).build()
            val response = client.newCall(request).execute()
            val code = response.code
            response.body?.string() // consume body to release connection
            codeToResult(code)
        } catch (e: Exception) {
            KeyCheckResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun checkClaude(apiKey: String): KeyCheckResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext KeyCheckResult.Invalid("No key entered")
        try {
            val body = """{"model":"claude-haiku-4-5-20251001","max_tokens":1,"messages":[{"role":"user","content":"hi"}]}"""
            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .post(body.toRequestBody(json))
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .build()
            val response = client.newCall(request).execute()
            val code = response.code
            response.body?.string()
            codeToResult(code)
        } catch (e: Exception) {
            KeyCheckResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun checkOpenAiCompatible(
        baseUrl: String,
        model: String,
        apiKey: String
    ): KeyCheckResult = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) return@withContext KeyCheckResult.Invalid("No URL entered")
        if (model.isBlank()) return@withContext KeyCheckResult.Invalid("No model entered")
        try {
            val body = """{"model":"$model","messages":[{"role":"user","content":"hi"}],"max_tokens":1}"""
            var url = baseUrl.trimEnd('/')
            if (!url.endsWith("/chat/completions")) {
                url = if (url.endsWith("/v1")) "$url/chat/completions"
                else if (!url.contains("/v1")) "$url/v1/chat/completions"
                else "$url/chat/completions"
            }
            val requestBuilder = Request.Builder()
                .url(url)
                .post(body.toRequestBody(json))
                .addHeader("Content-Type", "application/json")
            if (apiKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
            }
            val response = client.newCall(requestBuilder.build()).execute()
            val code = response.code
            response.body?.string()
            when (code) {
                200 -> KeyCheckResult.Valid
                401 -> KeyCheckResult.Invalid("Unauthorized")
                403 -> KeyCheckResult.Invalid("Forbidden")
                404 -> KeyCheckResult.Invalid("Endpoint not found")
                429 -> KeyCheckResult.Valid
                else -> KeyCheckResult.Invalid("HTTP $code")
            }
        } catch (e: Exception) {
            KeyCheckResult.Error(e.message ?: "Connection failed")
        }
    }

    suspend fun checkWeather(apiKey: String): KeyCheckResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext KeyCheckResult.Invalid("No key entered")
        try {
            val url = "https://api.openweathermap.org/data/2.5/weather?lat=13.75&lon=100.5&appid=$apiKey&units=metric"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val code = response.code
            response.body?.string()
            codeToResult(code)
        } catch (e: Exception) {
            KeyCheckResult.Error(e.message ?: "Network error")
        }
    }

    private fun codeToResult(code: Int): KeyCheckResult = when (code) {
        200 -> KeyCheckResult.Valid
        400 -> KeyCheckResult.Invalid("Bad request — check key format")
        401 -> KeyCheckResult.Invalid("Invalid API key")
        403 -> KeyCheckResult.Invalid("Forbidden — check permissions")
        429 -> KeyCheckResult.Valid // rate limited but key IS valid
        else -> KeyCheckResult.Invalid("HTTP $code")
    }
}
