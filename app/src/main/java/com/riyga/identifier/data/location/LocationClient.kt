package com.riyga.identifier.data.location

import com.riyga.identifier.data.models.LocationData
import kotlinx.coroutines.flow.Flow

interface LocationClient {
    fun getLocationUpdates(interval: Long): Flow<LocationData>

    suspend fun getCurrentLocation(): LocationData

    companion object {
        const val BACKGROUND_UPDATE_INTERVAL = 15L // Minutes
    }
}