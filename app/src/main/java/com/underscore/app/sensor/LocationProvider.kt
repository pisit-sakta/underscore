package com.underscore.app.sensor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.underscore.app.debug.AppLog
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class LocationUpdate(
    val latitude: Double,
    val longitude: Double,
    val speedMps: Float,
    val accuracy: Float
)

class LocationProvider(private val context: Context) {

    companion object {
        private const val TAG = "LocationProvider"
    }

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        5_000L // 5 second interval
    ).apply {
        setMinUpdateIntervalMillis(3_000L)
        // No minDistance filter — need updates even when stationary for weather/places
    }.build()

    fun locationUpdates(): Flow<LocationUpdate> = callbackFlow {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            AppLog.e(TAG, "Location permission NOT granted — emitting default and waiting")
            // Don't crash the flow — emit a zero location so the pipeline can still run
            trySend(LocationUpdate(0.0, 0.0, 0f, 0f))
            awaitClose { }
            return@callbackFlow
        }

        AppLog.d(TAG, "Starting location updates (interval=5s, minDistance=5m)")

        // Emit last known location immediately so combine() doesn't block
        try {
            fusedClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    AppLog.d(TAG, "Last known location: ${location.latitude}, ${location.longitude}")
                    trySend(
                        LocationUpdate(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            speedMps = if (location.hasSpeed()) location.speed else 0f,
                            accuracy = location.accuracy
                        )
                    )
                } else {
                    AppLog.d(TAG, "No last known location, emitting default")
                    trySend(LocationUpdate(0.0, 0.0, 0f, 0f))
                }
            }
        } catch (e: Exception) {
            AppLog.w(TAG, "Failed to get last location: ${e.message}")
            trySend(LocationUpdate(0.0, 0.0, 0f, 0f))
        }

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(
                        LocationUpdate(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            speedMps = if (location.hasSpeed()) location.speed else 0f,
                            accuracy = location.accuracy
                        )
                    )
                }
            }
        }

        fusedClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())

        awaitClose {
            AppLog.d(TAG, "Stopping location updates")
            fusedClient.removeLocationUpdates(callback)
        }
    }
}
