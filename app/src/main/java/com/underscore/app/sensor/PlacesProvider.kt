package com.underscore.app.sensor

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class NearbyPlace(
    val name: String,
    val types: List<String>,
    val vicinity: String
)

data class PlacesResult(
    val placeType: String,           // Primary detected type: "gym", "restaurant", "temple", etc.
    val zoneCharacter: String,       // Zone feel: "commercial", "residential", "nightlife", etc.
    val nearbyLandmarks: List<String>,
    val rawTypes: List<String>       // All detected Google place types
)

// Google Places Nearby Search response models
data class PlacesApiResponse(
    val results: List<PlacesApiResult>?,
    val status: String
)

data class PlacesApiResult(
    val name: String?,
    val types: List<String>?,
    val vicinity: String?,
    @SerializedName("business_status") val businessStatus: String?
)

class PlacesProvider(private val apiKey: String) : NearbyPlacesProvider {

    companion object {
        private const val TAG = "PlacesProvider"
        private const val BASE_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
        private const val SEARCH_RADIUS_M = 80 // meters
        private const val CACHE_DISTANCE_M = 100.0 // re-query after moving this far
        const val DEFAULT_API_KEY = "YOUR_GOOGLE_PLACES_API_KEY_HERE"
    }

    private val client = OkHttpClient()
    private val gson = Gson()

    private var cachedResult: PlacesResult? = null
    private var cachedLat: Double = 0.0
    private var cachedLng: Double = 0.0

    override suspend fun getNearbyPlaces(lat: Double, lng: Double): PlacesResult? {
        // Return cached if user hasn't moved far enough
        if (cachedResult != null && distanceMeters(lat, lng, cachedLat, cachedLng) < CACHE_DISTANCE_M) {
            return cachedResult
        }

        if (apiKey == DEFAULT_API_KEY) return null

        return withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL?location=$lat,$lng&radius=$SEARCH_RADIUS_M&key=$apiKey"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.w(TAG, "Places API returned ${response.code}")
                    return@withContext null
                }

                val body = response.body?.string() ?: return@withContext null
                val data = gson.fromJson(body, PlacesApiResponse::class.java)

                if (data.status != "OK" || data.results.isNullOrEmpty()) {
                    Log.d(TAG, "Places API: ${data.status}, ${data.results?.size ?: 0} results")
                    return@withContext null
                }

                val result = parseResults(data.results)
                cachedResult = result
                cachedLat = lat
                cachedLng = lng
                Log.d(TAG, "Place: ${result.placeType}, zone: ${result.zoneCharacter}")
                result
            } catch (e: Exception) {
                Log.e(TAG, "Places API failed", e)
                null
            }
        }
    }

    private fun parseResults(results: List<PlacesApiResult>): PlacesResult {
        // Collect all types from nearby places
        val allTypes = results.flatMap { it.types ?: emptyList() }
        val typeFrequency = allTypes.groupingBy { it }.eachCount()
        val landmarks = results.mapNotNull { it.name }.take(5)

        val placeType = classifyPrimaryType(typeFrequency)
        val zoneCharacter = classifyZoneCharacter(typeFrequency)

        return PlacesResult(
            placeType = placeType,
            zoneCharacter = zoneCharacter,
            nearbyLandmarks = landmarks,
            rawTypes = typeFrequency.keys.toList()
        )
    }

    private fun classifyPrimaryType(types: Map<String, Int>): String {
        // Priority-ordered: check for specific meaningful place types first
        val priorityMap = listOf(
            listOf("gym", "fitness_center") to "gym",
            listOf("temple", "church", "mosque", "synagogue", "hindu_temple", "place_of_worship") to "temple",
            listOf("hospital", "doctor", "dentist") to "hospital",
            listOf("university", "school") to "school",
            listOf("park", "campground") to "park",
            listOf("train_station", "transit_station", "bus_station", "subway_station", "airport") to "transit_hub",
            listOf("night_club", "bar") to "nightlife",
            listOf("restaurant", "food", "meal_delivery", "meal_takeaway", "bakery", "cafe") to "restaurant",
            listOf("shopping_mall", "department_store", "clothing_store", "shoe_store") to "shopping",
            listOf("convenience_store", "supermarket", "grocery_or_supermarket") to "convenience_store",
            listOf("bank", "atm", "finance") to "bank",
            listOf("gas_station") to "gas_station",
            listOf("lodging", "hotel") to "hotel",
            listOf("movie_theater", "amusement_park", "stadium", "museum", "art_gallery") to "entertainment",
            listOf("office", "accounting", "insurance_agency", "lawyer") to "office"
        )

        for ((googleTypes, label) in priorityMap) {
            if (googleTypes.any { types.containsKey(it) }) {
                return label
            }
        }

        return "urban" // default fallback
    }

    private fun classifyZoneCharacter(types: Map<String, Int>): String {
        val score = mutableMapOf<String, Int>()

        val zoneMapping = mapOf(
            "commercial" to listOf("store", "shop", "shopping_mall", "department_store",
                "clothing_store", "shoe_store", "jewelry_store", "supermarket",
                "grocery_or_supermarket", "convenience_store"),
            "nightlife" to listOf("night_club", "bar", "liquor_store"),
            "dining" to listOf("restaurant", "food", "cafe", "bakery", "meal_delivery",
                "meal_takeaway"),
            "residential" to listOf("neighborhood", "sublocality"),
            "cultural" to listOf("temple", "church", "mosque", "museum", "art_gallery",
                "library", "place_of_worship", "hindu_temple"),
            "nature" to listOf("park", "campground", "natural_feature"),
            "transit" to listOf("train_station", "transit_station", "bus_station",
                "subway_station", "airport", "taxi_stand"),
            "institutional" to listOf("hospital", "school", "university", "local_government_office",
                "courthouse", "police", "fire_station")
        )

        for ((zone, googleTypes) in zoneMapping) {
            val matches = googleTypes.count { types.containsKey(it) }
            if (matches > 0) score[zone] = matches
        }

        return score.maxByOrNull { it.value }?.key ?: "urban"
    }

    private fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
