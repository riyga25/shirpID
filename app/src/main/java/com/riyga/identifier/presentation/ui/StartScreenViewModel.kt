package com.riyga.identifier.presentation.ui

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.riyga.identifier.utils.isPermissionGranted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StartScreenViewModel(application: Application) : AndroidViewModel(application) {

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
                        getApplication(),
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                )
            }
        }
    }

    fun refreshPermissionsState() {
        val context = getApplication<Application>()
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
        activity: Activity, // Activity нужна для shouldShowRequestPermissionRationale
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
        } else {
            val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                currentlyRequesting
            )
            _uiState.update {
                it.copy(showSettingsDialog = !shouldShowRationale, isRequestingPermission = null)
            }
        }
        // После обработки результата сбрасываем isRequestingPermission
        // _uiState.update { it.copy(isRequestingPermission = null) } // Уже делается выше
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
}

data class PermissionState(
    val isAudioGranted: Boolean = false,
    val isFineLocationGranted: Boolean = false,
    val isNotificationsGranted: Boolean = true, // По умолчанию true для API < 33
    val showSettingsDialog: Boolean = false,
    val isRequestingPermission: String? = null // Хранит текущий запрашиваемый пермишен
) {
    val allPermissionsGranted: Boolean
        get() = isAudioGranted && isFineLocationGranted && isNotificationsGranted
}

sealed class StartScreenEvent {
    data class RequestPermission(val permission: String) : StartScreenEvent()
    object OpenAppSettings : StartScreenEvent()
}