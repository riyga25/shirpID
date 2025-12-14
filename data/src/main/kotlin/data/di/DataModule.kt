package data.di

import data.LabelsRepository
import data.LabelsRepositoryImpl
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import data.database.RecordRepository
import data.database.RecordRepositoryImpl
import data.database.AppDatabase
import data.location.LocationRepository
import data.location.LocationRepositoryImpl
import data.network.GeocoderApiService
import data.network.GeocoderDataSource
import data.network.GeocoderDataSourceImpl
import data.preferences.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit

val dataModule = module {
    single<CoroutineScope> {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    single { AppDatabase.getDatabase(androidContext()) }
    single { AppPreferences(androidContext()) }
    single<LabelsRepository> { LabelsRepositoryImpl(androidContext(), get(), get()) }
    single { get<AppDatabase>().recordDao() }
    single<RecordRepository> { RecordRepositoryImpl(get()) }
    single<LocationRepository> { LocationRepositoryImpl(get()) }
    single { provideGeocoderApi() }
    single<GeocoderDataSource> { GeocoderDataSourceImpl(get()) }
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