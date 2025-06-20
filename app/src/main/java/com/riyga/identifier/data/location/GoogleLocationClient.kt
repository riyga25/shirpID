package com.riyga.identifier.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.riyga.identifier.data.models.LocationData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GoogleLocationClient(
    private val context: Context
) : LocationClient {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val settingsClient = LocationServices.getSettingsClient(context)

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(interval: Long): Flow<LocationData> = callbackFlow {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            interval
        ).apply {
            setWaitForAccurateLocation(true)
            setMinUpdateDistanceMeters(20f)
        }.build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.lastOrNull()?.let { location ->
                    trySend(location.toLocationData())
                }
            }
        }

        val task = settingsClient.checkLocationSettings(
            LocationSettingsRequest.Builder()
                .addLocationRequest(request)
                .build()
        )

        task.addOnSuccessListener {
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )
        }.addOnFailureListener { e ->
            e.printStackTrace()
            close(e)
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LocationData? = suspendCancellableCoroutine { cont ->
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            null
        ).addOnSuccessListener { location ->
            location?.let {
                cont.resume(it.toLocationData())
            } ?: cont.resume(null)
        }.addOnFailureListener { e ->
            cont.resumeWithException(e)
        }
    }

    private fun Location.toLocationData() = LocationData(
        latitude,
        longitude,
        accuracy,
        elapsedRealtimeNanos / 1_000_000
    )
}