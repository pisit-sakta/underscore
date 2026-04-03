package com.underscore.app.data

import android.util.Log

class KnownLocationManager(private val db: SongDatabase) {

    companion object {
        private const val TAG = "KnownLocationManager"
        private const val LEITMOTIF_THRESHOLD = 5 // visits before a leitmotif can develop
    }

    private var cachedLocations: List<KnownLocation> = emptyList()
    private var lastCacheTime: Long = 0
    private val cacheValidMs = 60_000L // 1 minute

    suspend fun checkLocation(lat: Double, lng: Double, placeType: String?): KnownLocation? {
        refreshCacheIfNeeded()

        // Find the nearest known location within its radius
        val match = cachedLocations.firstOrNull { loc ->
            distanceMeters(lat, lng, loc.latitude, loc.longitude) < loc.radiusMeters
        }

        if (match != null) {
            db.knownLocationDao().recordVisit(match.id)
            Log.d(TAG, "At known location: ${match.label} (visit #${match.visitCount + 1})")
            return match.copy(visitCount = match.visitCount + 1)
        }

        // Not at a known location — if we've been here before with same placeType, auto-create
        // (This will be called by the service when scoring; locations grow organically)
        return null
    }

    suspend fun registerLocation(
        label: String,
        lat: Double,
        lng: Double,
        placeType: String? = null,
        radiusMeters: Float = 100f
    ): KnownLocation {
        val location = KnownLocation(
            label = label,
            latitude = lat,
            longitude = lng,
            radiusMeters = radiusMeters,
            placeType = placeType
        )
        val id = db.knownLocationDao().insert(location)
        invalidateCache()
        Log.d(TAG, "Registered known location: $label")
        return location.copy(id = id)
    }

    suspend fun tryDevelopLeitmotif(locationId: Long): KnownLocation? {
        val location = cachedLocations.firstOrNull { it.id == locationId } ?: return null

        if (location.visitCount < LEITMOTIF_THRESHOLD) return null
        if (location.leitmotifUri != null) return location // Already has one

        // Find the most-played song at this location's place type
        val placeType = location.placeType ?: return null
        val topSongs = db.sceneHistoryDao().getTopSongsForPlace(placeType, limit = 1)
        val topSong = topSongs.firstOrNull() ?: return null

        // Look up the song details
        val song = db.taggedSongDao().getByUri(topSong.songUri) ?: return null

        db.knownLocationDao().setLeitmotif(location.id, song.spotifyUri, song.title)
        invalidateCache()

        Log.d(TAG, "Leitmotif developed for ${location.label}: ${song.title} by ${song.artist}")
        return location.copy(leitmotifUri = song.spotifyUri, leitmotifTitle = song.title)
    }

    suspend fun getAllLocations(): List<KnownLocation> {
        refreshCacheIfNeeded()
        return cachedLocations
    }

    private suspend fun refreshCacheIfNeeded() {
        if (System.currentTimeMillis() - lastCacheTime > cacheValidMs) {
            cachedLocations = db.knownLocationDao().getAll()
            lastCacheTime = System.currentTimeMillis()
        }
    }

    private fun invalidateCache() {
        lastCacheTime = 0
    }

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
