package org.tensorflow.lite.examples.soundclassifier;

import android.Manifest;
import android.annotation.SuppressLint
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationHelper {
    private var locationListener: LocationListener? = null

    suspend fun requestLocation(context: Context, soundClassifier: SoundClassifier) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && checkLocationProvider(context)
        ) {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            try {
                val location = getLocation(locationManager)
                soundClassifier.runMetaInterpreter(location)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLocation(locationManager: LocationManager): Location =
        suspendCancellableCoroutine { continuation ->
            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    continuation.resume(location)
                    locationManager.removeUpdates(this)
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

                override fun onProviderEnabled(provider: String) {}

                override fun onProviderDisabled(provider: String) {
                    continuation.resumeWithException(IllegalStateException("Provider disabled"))
                    locationManager.removeUpdates(this)
                }
            }

            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                60000L,
                0f,
                locationListener!!
            )

            continuation.invokeOnCancellation {
                locationManager.removeUpdates(locationListener!!)
            }
        }

    fun stopLocation(context: Context) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationListener?.let {
            locationManager.removeUpdates(it)
            locationListener = null
        }
    }

    private fun checkLocationProvider(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

}