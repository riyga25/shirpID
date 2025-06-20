package com.riyga.identifier.data.location

import com.riyga.identifier.data.models.LocationData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

interface LocationRepository {
    val locationUpdates: Flow<LocationData>
    suspend fun getCurrentLocation(): LocationData?
    fun startBackgroundUpdates()
    fun stopBackgroundUpdates()
}

class LocationRepositoryImpl(
    private val locationClient: LocationClient
) : LocationRepository {
    private val interval = (1000 * 60 * 1).toLong()

    override val locationUpdates = locationClient
        .getLocationUpdates(interval)
        .shareIn(
            CoroutineScope(Dispatchers.Default + SupervisorJob()),
            SharingStarted.WhileSubscribed(5000),
            1
        )

    override suspend fun getCurrentLocation(): LocationData? {
        return locationClient.getCurrentLocation()
    }

    override fun startBackgroundUpdates() {
        // Custom logic for foreground service if needed
    }

    override fun stopBackgroundUpdates() {
        // Stop updates implementation
    }
}