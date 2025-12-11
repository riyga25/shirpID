package by.riyga.shirpid.presentation.ui.start

import android.Manifest
import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.riyga.shirpid.data.location.LocationRepository
import by.riyga.shirpid.data.location.LocationUnavailableException
import by.riyga.shirpid.data.models.LocationData
import by.riyga.shirpid.utils.isPermissionGranted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StartScreenViewModel(
    private val context: Context,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionState())
    val uiState: StateFlow<PermissionState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<StartScreenEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        refreshPermissionsState()
        // Устанавливаем isNotificationsGranted в false если API >= 33 и разрешение не дано
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            _uiState.update {
                it.copy(
                    isNotificationsGranted = isPermissionGranted(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                )
            }
        }
    }

    fun refreshPermissionsState() {
        _uiState.update { currentState ->
            currentState.copy(
                isAudioGranted = isPermissionGranted(context, Manifest.permission.RECORD_AUDIO),
                isFineLocationGranted = isPermissionGranted(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                isNotificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    isPermissionGranted(context, Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    true
                }
            )
        }
    }

    fun onPermissionRequested(permission: String) {
        viewModelScope.launch {
            _eventFlow.emit(StartScreenEvent.RequestPermission(permission))
        }
        _uiState.update { it.copy(isRequestingPermission = permission) }
    }

    fun onPermissionResult(
        activity: Activity,
        isGranted: Boolean
    ) {
        val currentlyRequesting = _uiState.value.isRequestingPermission ?: return

        if (isGranted) {
            _uiState.update {
                when (currentlyRequesting) {
                    Manifest.permission.RECORD_AUDIO -> it.copy(isAudioGranted = true)
                    Manifest.permission.ACCESS_FINE_LOCATION -> it.copy(isFineLocationGranted = true)
                    Manifest.permission.POST_NOTIFICATIONS -> it.copy(isNotificationsGranted = true)
                    else -> it
                }.copy(showSettingsDialog = false, isRequestingPermission = null)
            }

            if (uiState.value.allPermissionsGranted) {
                fetchLocationAndProceed()
            }
        } else {
            val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                currentlyRequesting
            )
            _uiState.update {
                it.copy(showSettingsDialog = !shouldShowRationale, isRequestingPermission = null)
            }
        }
    }

    fun dismissSettingsDialog() {
        _uiState.update { it.copy(showSettingsDialog = false) }
    }

    fun requestOpenAppSettings() {
        viewModelScope.launch {
            _eventFlow.emit(StartScreenEvent.OpenAppSettings)
        }
        dismissSettingsDialog() // Закрываем диалог после запроса на открытие настроек
    }

    fun fetchLocationAndProceed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLocation = true, locationError = null) }
            try {
                val location = locationRepository.getCurrentLocation()
                _uiState.update {
                    it.copy(
                        currentLocation = location,
                        isLoadingLocation = false
                    )
                }
            } catch (e: SecurityException) {
                _uiState.update {
                    it.copy(
                        locationError = "Location permission is required to get location.",
                        isLoadingLocation = false
                    )
                }
                // Возможно, нужно снова показать диалог настроек или запрос разрешения
            } catch (e: LocationUnavailableException) {
                _uiState.update {
                    it.copy(
                        locationError = e.message ?: "Could not retrieve location.",
                        isLoadingLocation = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        locationError = "An unexpected error occurred while fetching location.",
                        isLoadingLocation = false
                    )
                }
            }
        }
    }
}

data class PermissionState(
    val isAudioGranted: Boolean = false,
    val isFineLocationGranted: Boolean = false,
    val isNotificationsGranted: Boolean = true, // По умолчанию true для API < 33
    val showSettingsDialog: Boolean = false,
    val isRequestingPermission: String? = null, // Хранит текущий запрашиваемый пермишен
    val currentLocation: LocationData? = null,
    val isLoadingLocation: Boolean = false,
    val locationError: String? = null
) {
    val allPermissionsGranted: Boolean
        get() = isAudioGranted && isFineLocationGranted && isNotificationsGranted
}

sealed class StartScreenEvent {
    data class RequestPermission(val permission: String) : StartScreenEvent()
    object OpenAppSettings : StartScreenEvent()
}