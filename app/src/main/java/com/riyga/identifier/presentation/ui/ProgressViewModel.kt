package com.riyga.identifier.presentation.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riyga.identifier.data.location.LocationRepository
import com.riyga.identifier.data.models.LocationData
import com.riyga.identifier.data.network.GeocoderDataSource
import com.riyga.identifier.presentation.models.LocationInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProgressViewModel(
    private val geocoderDataSource: GeocoderDataSource,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState: MutableStateFlow<IdentifierState> by lazy {
        MutableStateFlow(
            IdentifierState()
        )
    }
    val uiState by lazy { _uiState.asStateFlow() }

    init {
        fetchLocation()
    }

    private fun fetchLocation() {
        viewModelScope.launch {
            val loc = locationRepository.getCurrentLocation()

            _uiState.value = _uiState.value.copy(location = loc)

            if (loc != null) {
                getLocationInfo(loc)
            }
        }
    }

    private fun getLocationInfo(location: LocationData) {
        viewModelScope.launch {
            try {
                val locationInfo = geocoderDataSource.getLocationInfo(
                    location.latitude,
                    location.longitude
                )
                _uiState.value = _uiState.value.copy(locationInfo = locationInfo)
            } catch (err: Throwable) {
                Log.e("APP", err.localizedMessage)
            }
        }
    }
}

data class IdentifierState(
    val locationInfo: LocationInfo? = null,
    val location: LocationData? = null
)