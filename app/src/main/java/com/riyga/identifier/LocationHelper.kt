package com.riyga.identifier;

import android.annotation.SuppressLint
import android.content.Context;
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

class LocationHelper(context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices
        .getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission") // Убедитесь, что вы проверили разрешения перед вызовом этой функции
    suspend fun getCurrentLocation(): Location? {
        return try {
            // Попытка получить последнее известное местоположение
            fusedLocationClient.lastLocation.await() ?: run {
                // Если последнее местоположение недоступно, запросите обновленное местоположение
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}