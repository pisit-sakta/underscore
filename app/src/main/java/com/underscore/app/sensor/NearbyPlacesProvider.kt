package com.underscore.app.sensor

interface NearbyPlacesProvider {
    suspend fun getNearbyPlaces(lat: Double, lng: Double): PlacesResult?
}
