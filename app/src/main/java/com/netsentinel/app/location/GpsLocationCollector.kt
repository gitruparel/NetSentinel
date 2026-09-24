package com.netsentinel.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.netsentinel.app.utils.PermissionManager
import kotlinx.coroutines.tasks.await

class GpsLocationCollector(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Pair<Double, Double> {
        if (!PermissionManager.hasLocationPermission(context)) {
            return Pair(37.7749, -122.4194) // Fallback default
        }

        return try {
            val location: Location? = fusedLocationClient.lastLocation.await()
            if (location != null) {
                Pair(location.latitude, location.longitude)
            } else {
                val freshLocation: Location? = fusedLocationClient.getCurrentLocation(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).await()
                if (freshLocation != null) {
                    Pair(freshLocation.latitude, freshLocation.longitude)
                } else {
                    Pair(37.7749, -122.4194)
                }
            }
        } catch (e: Exception) {
            Pair(37.7749, -122.4194)
        }
    }
}
