package by.riyga.shirpid.data.network

import by.riyga.shirpid.data.models.GeoDateInfo
import by.riyga.shirpid.data.preferences.AppPreferences
import kotlinx.coroutines.flow.first

interface GeocoderDataSource {
    suspend fun getLocationInfo(latitude: Double, longitude: Double): GeoDateInfo
}

internal class GeocoderDataSourceImpl(
    private val apiService: GeocoderApiService,
    private val appPreferences: AppPreferences
) : GeocoderDataSource {

    private val apiKey: String = "pk.8f4dd058bb97d931527924d3ae1b908f"

    override suspend fun getLocationInfo(latitude: Double, longitude: Double): GeoDateInfo {
        return try {
            val language = appPreferences.language.first()
            val response = apiService.reverseGeocoding(
                apiKey = apiKey,
                latitude = latitude,
                longitude = longitude,
                language = language.code
            )
            response.toDomain()
        } catch (e: Exception) {
            throw Throwable("Failed to fetch location data", e)
        }
    }
}