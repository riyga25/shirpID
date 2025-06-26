package com.riyga.identifier.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.riyga.identifier.data.location.LocationRepository
import com.riyga.identifier.data.location.LocationRepositoryImpl
import com.riyga.identifier.data.network.GeocoderApiService
import com.riyga.identifier.data.network.GeocoderDataSource
import com.riyga.identifier.data.network.GeocoderDataSourceImpl
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit

val dataModule = module {
    single { provideGeocoderApi() }
    single<GeocoderDataSource> { GeocoderDataSourceImpl(get()) }
    single<LocationRepository> { LocationRepositoryImpl(get()) }
}

private val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}

private fun provideGeocoderApi(): GeocoderApiService {
    val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        )
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://us1.locationiq.com/")
        .addConverterFactory(
            json.asConverterFactory("application/json".toMediaType())
        )
        .client(client)
        .build()

    return retrofit.create(GeocoderApiService::class.java)
}