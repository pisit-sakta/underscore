package com.underscore.app.sensor

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Free replacement for Google Places API using OpenStreetMap Overpass API.
 * Returns the same PlacesResult format so the downstream pipeline is untouched.
 */
class OverpassPlacesProvider : NearbyPlacesProvider {

    companion object {
        private const val TAG = "OverpassPlaces"
        private const val SEARCH_RADIUS_M = 80
        private const val CACHE_DISTANCE_M = 150.0   // wider than Google's 100m
        private const val CACHE_COOLDOWN_MS = 30_000L // 30s between API calls
    }

    private val endpoints = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    // Two-layer cache: distance + time
    private var cachedResult: PlacesResult? = null
    private var cachedLat: Double = 0.0
    private var cachedLng: Double = 0.0
    private var cachedAtMs: Long = 0L

    override suspend fun getNearbyPlaces(lat: Double, lng: Double): PlacesResult? {
        val now = System.currentTimeMillis()

        // Return cached if user hasn't moved far enough AND cooldown hasn't expired
        if (cachedResult != null &&
            distanceMeters(lat, lng, cachedLat, cachedLng) < CACHE_DISTANCE_M &&
            now - cachedAtMs < CACHE_COOLDOWN_MS
        ) {
            return cachedResult
        }

        // Also enforce cooldown even if distance threshold is exceeded
        if (now - cachedAtMs < CACHE_COOLDOWN_MS) {
            return cachedResult
        }

        return withContext(Dispatchers.IO) {
            val query = buildQuery(lat, lng)
            val result = tryEndpoints(query)

            if (result != null) {
                cachedResult = result
                cachedLat = lat
                cachedLng = lng
                cachedAtMs = System.currentTimeMillis()
                Log.d(TAG, "Place: ${result.placeType}, zone: ${result.zoneCharacter}")
            }
            result ?: cachedResult // return stale cache on failure
        }
    }

    private fun buildQuery(lat: Double, lng: Double): String {
        val r = SEARCH_RADIUS_M
        return """
            [out:json][timeout:5];
            (
              node(around:$r,$lat,$lng)["amenity"];
              node(around:$r,$lat,$lng)["shop"];
              node(around:$r,$lat,$lng)["leisure"];
              node(around:$r,$lat,$lng)["tourism"];
              node(around:$r,$lat,$lng)["office"];
              node(around:$r,$lat,$lng)["public_transport"];
              node(around:$r,$lat,$lng)["railway"];
              way(around:$r,$lat,$lng)["amenity"];
              way(around:$r,$lat,$lng)["shop"];
              way(around:$r,$lat,$lng)["leisure"];
              way(around:$r,$lat,$lng)["landuse"];
            );
            out tags;
        """.trimIndent()
    }

    private fun tryEndpoints(query: String): PlacesResult? {
        for (endpoint in endpoints) {
            try {
                val body = FormBody.Builder()
                    .add("data", query)
                    .build()

                val request = Request.Builder()
                    .url(endpoint)
                    .post(body)
                    .header("User-Agent", "Underscore-App/1.0")
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.w(TAG, "$endpoint returned ${response.code}")
                    response.close()
                    continue
                }

                val json = response.body?.string() ?: continue
                val data = gson.fromJson(json, OverpassResponse::class.java)
                val elements = data.elements ?: emptyList()

                if (elements.isEmpty()) {
                    Log.d(TAG, "$endpoint: 0 elements returned")
                    // Valid response, just no POIs nearby — return urban default
                    return PlacesResult(
                        placeType = "urban",
                        zoneCharacter = "urban",
                        nearbyLandmarks = emptyList(),
                        rawTypes = emptyList()
                    )
                }

                return parseElements(elements)
            } catch (e: Exception) {
                Log.w(TAG, "$endpoint failed: ${e.message}")
            }
        }
        Log.e(TAG, "All Overpass endpoints failed")
        return null
    }

    // ── Response models ──

    private data class OverpassResponse(
        val elements: List<OverpassElement>?
    )

    private data class OverpassElement(
        val type: String?,
        val id: Long?,
        val tags: Map<String, String>?
    )

    // ── Parsing ──

    private fun parseElements(elements: List<OverpassElement>): PlacesResult {
        // Extract all relevant "key=value" tags and count them
        val relevantKeys = setOf(
            "amenity", "shop", "leisure", "tourism", "office",
            "public_transport", "railway", "landuse", "building", "natural"
        )

        val tagFrequency = mutableMapOf<String, Int>()
        val landmarks = mutableListOf<String>()

        for (element in elements) {
            val tags = element.tags ?: continue
            for ((key, value) in tags) {
                if (key in relevantKeys) {
                    val tag = "$key=$value"
                    tagFrequency[tag] = (tagFrequency[tag] ?: 0) + 1
                }
            }
            // Collect names for landmarks
            tags["name"]?.let { name ->
                if (name !in landmarks) landmarks.add(name)
            }
        }

        val placeType = classifyPrimaryType(tagFrequency)
        val zoneCharacter = classifyZoneCharacter(tagFrequency)

        return PlacesResult(
            placeType = placeType,
            zoneCharacter = zoneCharacter,
            nearbyLandmarks = landmarks.take(5),
            rawTypes = tagFrequency.keys.toList()
        )
    }

    // ── Classification (mirrors PlacesProvider's priority order) ──

    private fun classifyPrimaryType(tags: Map<String, Int>): String {
        // Priority-ordered, same as Google version
        val priorityMap = listOf(
            listOf("amenity=gym", "leisure=fitness_centre", "leisure=sports_centre") to "gym",
            listOf("amenity=place_of_worship", "building=temple", "building=church",
                "building=mosque") to "temple",
            listOf("amenity=hospital", "amenity=clinic", "amenity=doctors",
                "amenity=dentist") to "hospital",
            listOf("amenity=university", "amenity=school", "amenity=college") to "school",
            listOf("leisure=park", "leisure=garden", "leisure=nature_reserve") to "park",
            listOf("public_transport=station", "public_transport=stop_position",
                "railway=station", "railway=halt", "amenity=bus_station",
                "amenity=ferry_terminal") to "transit_hub",
            listOf("amenity=nightclub", "amenity=bar", "amenity=pub") to "nightlife",
            listOf("amenity=restaurant", "amenity=cafe", "amenity=fast_food",
                "amenity=food_court", "amenity=ice_cream", "shop=bakery") to "restaurant",
            listOf("shop=mall", "shop=department_store", "shop=clothes",
                "shop=shoes", "shop=jewelry") to "shopping",
            listOf("shop=convenience", "shop=supermarket",
                "shop=grocery") to "convenience_store",
            listOf("amenity=bank", "amenity=atm", "amenity=bureau_de_change") to "bank",
            listOf("amenity=fuel") to "gas_station",
            listOf("tourism=hotel", "tourism=hostel", "tourism=motel",
                "tourism=guest_house") to "hotel",
            listOf("amenity=cinema", "amenity=theatre", "tourism=museum",
                "tourism=gallery", "leisure=stadium",
                "leisure=amusement_arcade") to "entertainment",
            listOf("office=company", "office=government", "office=insurance",
                "office=lawyer", "office=it", "office=financial") to "office"
        )

        for ((osmTags, label) in priorityMap) {
            if (osmTags.any { tags.containsKey(it) }) {
                return label
            }
        }

        // Catch-all for any office=* tag
        if (tags.keys.any { it.startsWith("office=") }) return "office"

        return "urban"
    }

    private fun classifyZoneCharacter(tags: Map<String, Int>): String {
        val score = mutableMapOf<String, Int>()

        val zoneMapping = mapOf(
            "commercial" to { t: Map<String, Int> ->
                t.keys.count { it.startsWith("shop=") }
            },
            "nightlife" to { t: Map<String, Int> ->
                listOf("amenity=nightclub", "amenity=bar", "amenity=pub")
                    .count { t.containsKey(it) }
            },
            "dining" to { t: Map<String, Int> ->
                listOf("amenity=restaurant", "amenity=cafe", "amenity=fast_food",
                    "amenity=food_court", "shop=bakery")
                    .count { t.containsKey(it) }
            },
            "residential" to { t: Map<String, Int> ->
                listOf("building=residential", "building=apartments",
                    "landuse=residential")
                    .count { t.containsKey(it) }
            },
            "cultural" to { t: Map<String, Int> ->
                listOf("amenity=place_of_worship", "tourism=museum",
                    "tourism=gallery", "amenity=library", "building=temple",
                    "building=church")
                    .count { t.containsKey(it) }
            },
            "nature" to { t: Map<String, Int> ->
                listOf("leisure=park", "leisure=garden", "leisure=nature_reserve")
                    .count { t.containsKey(it) } +
                    t.keys.count { it.startsWith("natural=") }
            },
            "transit" to { t: Map<String, Int> ->
                t.keys.count { it.startsWith("public_transport=") || it.startsWith("railway=") } +
                    listOf("amenity=bus_station").count { t.containsKey(it) }
            },
            "institutional" to { t: Map<String, Int> ->
                listOf("amenity=hospital", "amenity=clinic", "amenity=school",
                    "amenity=university", "amenity=college", "amenity=police",
                    "amenity=fire_station", "amenity=courthouse")
                    .count { t.containsKey(it) }
            }
        )

        for ((zone, scorer) in zoneMapping) {
            val matches = scorer(tags)
            if (matches > 0) score[zone] = matches
        }

        return score.maxByOrNull { it.value }?.key ?: "urban"
    }

    // ── Haversine distance ──

    private fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
