package com.riyga.identifier.data.network

import com.riyga.identifier.presentation.models.LocationInfo

interface GeocoderDataSource {
    suspend fun getLocationInfo(latitude: Double, longitude: Double): LocationInfo
}

internal class GeocoderDataSourceImpl(
    private val apiService: GeocoderApiService
) : GeocoderDataSource {

    private val apiKey: String = "pk.8f4dd058bb97d931527924d3ae1b908f"

    override suspend fun getLocationInfo(latitude: Double, longitude: Double): LocationInfo {
        return try {
            val response = apiService.reverseGeocoding(
                apiKey = apiKey,
                latitude = latitude,
                longitude = longitude
            )
            response.toDomain()
        } catch (e: Exception) {
            throw Throwable("Failed to fetch location data", e)
        }
    }
}