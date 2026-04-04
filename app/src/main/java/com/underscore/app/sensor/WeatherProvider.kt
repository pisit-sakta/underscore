package com.underscore.app.sensor

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class WeatherState(
    val condition: String,
    val description: String,
    val temperatureC: Float,
    val humidity: Int
)

data class OpenWeatherResponse(
    val weather: List<OpenWeatherCondition>,
    val main: OpenWeatherMain
)

data class OpenWeatherCondition(
    val id: Int,
    val main: String,
    val description: String
)

data class OpenWeatherMain(
    val temp: Float,
    val humidity: Int
)

class WeatherProvider(private val apiKey: String) {

    companion object {
        private const val TAG = "WeatherProvider"
        const val DEFAULT_API_KEY = "YOUR_OPENWEATHER_API_KEY_HERE"
        private const val BASE_URL = "https://api.openweathermap.org/data/2.5/weather"
    }

    private val client = OkHttpClient()
    private val gson = Gson()

    private var cachedWeather: WeatherState? = null
    private var lastFetchTime: Long = 0
    private val cacheDurationMs = 10 * 60 * 1000L

    suspend fun getWeather(lat: Double, lon: Double): WeatherState? {
        if (cachedWeather != null && System.currentTimeMillis() - lastFetchTime < cacheDurationMs) {
            return cachedWeather
        }

        if (apiKey == DEFAULT_API_KEY || apiKey.isBlank()) {
            Log.d(TAG, "No weather API key configured — skipping")
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL?lat=$lat&lon=$lon&appid=$apiKey&units=metric"
                Log.d(TAG, "Fetching weather for ${"%.3f".format(lat)}, ${"%.3f".format(lon)}")

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()?.take(300)
                    Log.e(TAG, "Weather API ${response.code}: $errorBody")
                    return@withContext null
                }

                val body = response.body?.string()
                if (body == null) {
                    Log.e(TAG, "Weather API returned null body")
                    return@withContext null
                }

                val data = gson.fromJson(body, OpenWeatherResponse::class.java)
                val condition = data.weather.firstOrNull()
                if (condition == null) {
                    Log.w(TAG, "Weather response missing condition data: ${body.take(200)}")
                    return@withContext null
                }

                val weather = WeatherState(
                    condition = normalizeCondition(condition.main),
                    description = condition.description,
                    temperatureC = data.main.temp,
                    humidity = data.main.humidity
                )

                cachedWeather = weather
                lastFetchTime = System.currentTimeMillis()
                Log.d(TAG, "Weather: ${weather.condition} (${weather.description}), ${weather.temperatureC}°C")
                weather
            } catch (e: Exception) {
                Log.e(TAG, "Weather fetch failed: ${e.message}", e)
                null
            }
        }
    }

    private fun normalizeCondition(main: String): String = when (main.lowercase()) {
        "clear" -> "clear"
        "clouds" -> "clouds"
        "rain", "drizzle" -> "rain"
        "thunderstorm" -> "storm"
        "snow" -> "snow"
        "mist", "smoke", "haze", "dust", "fog", "sand", "ash", "squall", "tornado" -> "fog"
        else -> "clear"
    }
}
