package data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat.checkSelfPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import data.models.LocationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// Исключение для случаев, когда местоположение недоступно или разрешение отсутствует
class LocationUnavailableException(message: String) : Exception(message)

interface LocationRepository {
    /**
     * Асинхронно запрашивает текущее местоположение пользователя.
     * Возвращает [LocationData] в случае успеха.
     * Может выбросить [LocationUnavailableException], если местоположение недоступно
     * или разрешение отсутствует (хотя проверка разрешения должна быть выше).
     * @throws SecurityException если разрешение ACCESS_FINE_LOCATION или ACCESS_COARSE_LOCATION не предоставлено.
     * @throws LocationUnavailableException если местоположение не может быть определено.
     */
    suspend fun getCurrentLocation(): LocationData?
}

class LocationRepositoryImpl(
    private val context: Context // Теперь репозиторий зависит от Context для FusedLocationProviderClient
) : LocationRepository {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    @android.annotation.SuppressLint("MissingPermission") // Разрешения должны быть проверены перед вызовом
    override suspend fun getCurrentLocation(): LocationData? {
        // Проверка разрешения (дублирование проверки из ViewModel здесь для надежности,
        // но основная проверка должна быть на уровне UI/ViewModel)
        if (!hasLocationPermission()) {
            // Можно либо выбросить исключение, либо вернуть null,
            // но лучше, чтобы ViewModel обрабатывал отсутствие разрешения до вызова.
            // Для примера, вернем null, но ViewModel должен это учитывать.
            throw SecurityException("Location permission not granted.")
        }

        return withContext(Dispatchers.IO) { // Выполняем в IO диспатчере
            try {
                // Попытка получить последнее известное местоположение
                val lastKnownLocation = fusedLocationClient.lastLocation.await()
                if (lastKnownLocation != null) {
                    return@withContext LocationData(
                        latitude = lastKnownLocation.latitude,
                        longitude = lastKnownLocation.longitude,
                        accuracy = lastKnownLocation.accuracy,
                        timestamp = lastKnownLocation.time
                    )
                }

                // Если последнее местоположение недоступно, запросите обновленное местоположение
                // Используем getCurrentLocation с более высоким приоритетом
                val currentLocation = fusedLocationClient.getCurrentLocation(
                    PRIORITY_HIGH_ACCURACY, // или PRIORITY_BALANCED_POWER_ACCURACY если высокая точность не критична
                    null // Можно передать CancellationTokenSource().token если нужна возможность отмены
                )
                    .await() // await() может вернуть null, если местоположение не может быть определено

                currentLocation?.let {
                    LocationData(
                        latitude = it.latitude,
                        longitude = it.longitude,
                        accuracy = it.accuracy,
                        timestamp = it.time
                    )
                }
            } catch (e: SecurityException) {
                // Это исключение должно быть поймано, если разрешения не предоставлены,
                // но проверка выше должна это предотвратить.
                // Перебрасываем, чтобы ViewModel мог его обработать.
                throw e
            } catch (e: Exception) {
                // Другие возможные ошибки при получении местоположения (например, выключены службы геолокации)
                // e.printStackTrace() // Логирование ошибки
                throw LocationUnavailableException("Failed to get current location: ${e.message}")
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}