package by.riyga.shirpid.data.di

import by.riyga.shirpid.data.LabelsRepository
import by.riyga.shirpid.data.LabelsRepositoryImpl
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import by.riyga.shirpid.data.database.RecordRepository
import by.riyga.shirpid.data.database.RecordRepositoryImpl
import by.riyga.shirpid.data.database.AppDatabase
import by.riyga.shirpid.data.location.LocationRepository
import by.riyga.shirpid.data.location.LocationRepositoryImpl
import by.riyga.shirpid.data.network.GeocoderApiService
import by.riyga.shirpid.data.network.GeocoderDataSource
import by.riyga.shirpid.data.network.GeocoderDataSourceImpl
import by.riyga.shirpid.data.preferences.AppPreferences
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