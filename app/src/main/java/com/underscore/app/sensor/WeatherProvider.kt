package com.underscore.app.sensor

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class WeatherState(
    val condition: String,    // "clear", "clouds", "rain", "storm", "snow", "fog"
    val description: String,  // "light rain", "overcast clouds", etc.
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
        // ⚠️ REPLACE with your OpenWeatherMap API key
        // Get one free at https://openweathermap.org/api
        const val DEFAULT_API_KEY = "YOUR_OPENWEATHER_API_KEY_HERE"
        private const val BASE_URL = "https://api.openweathermap.org/data/2.5/weather"
    }

    private val client = OkHttpClient()
    private val gson = Gson()

    private var cachedWeather: WeatherState? = null
    private var lastFetchTime: Long = 0
    private val cacheDurationMs = 10 * 60 * 1000L // 10 minutes

    suspend fun getWeather(lat: Double, lon: Double): WeatherState? {
        // Return cached if fresh enough
        if (cachedWeather != null && System.currentTimeMillis() - lastFetchTime < cacheDurationMs) {
            return cachedWeather
        }

        if (apiKey == DEFAULT_API_KEY) return null

        return withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL?lat=$lat&lon=$lon&appid=$apiKey&units=metric"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) return@withContext null

                val body = response.body?.string() ?: return@withContext null
                val data = gson.fromJson(body, OpenWeatherResponse::class.java)
                val condition = data.weather.firstOrNull() ?: return@withContext null

                val weather = WeatherState(
                    condition = normalizeCondition(condition.main),
                    description = condition.description,
                    temperatureC = data.main.temp,
                    humidity = data.main.humidity
                )

                cachedWeather = weather
                lastFetchTime = System.currentTimeMillis()
                weather
            } catch (e: Exception) {
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
