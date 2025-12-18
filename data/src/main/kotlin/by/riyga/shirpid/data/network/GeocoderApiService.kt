package by.riyga.shirpid.data.network

import by.riyga.shirpid.data.models.LocationIQResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocoderApiService {
    @GET("v1/reverse")
    suspend fun reverseGeocoding(
        @Query("key") apiKey: String,
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("format") format: String = "json",
        @Query("accept-language") language: String = "ru"
    ): LocationIQResponse
}