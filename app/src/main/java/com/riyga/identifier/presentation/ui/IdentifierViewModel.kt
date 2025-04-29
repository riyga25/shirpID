package com.riyga.identifier.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riyga.identifier.data.location.LocationRepository
import com.riyga.identifier.data.models.LocationData
import com.riyga.identifier.data.network.GeocoderDataSource
import com.riyga.identifier.presentation.models.LocationInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IdentifierViewModel(
    private val geocoderDataSource: GeocoderDataSource,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState: MutableStateFlow<IdentifierState> by lazy {
        MutableStateFlow(
            IdentifierState()
        )
    }
    val uiState by lazy { _uiState.asStateFlow() }

    fun startTrackingLocation() {
        viewModelScope.launch {
            locationRepository.locationUpdates.collect {
                if (uiState.value.locationInfo == null) {
                    getLocationInfo(it)
                }

                _uiState.value = _uiState.value.copy(location = it)
            }
        }
    }

    private fun getLocationInfo(location: LocationData) {
        viewModelScope.launch {
            val locationInfo = geocoderDataSource.getLocationInfo(
                location.latitude,
                location.longitude
            )
            _uiState.value = _uiState.value.copy(locationInfo = locationInfo)
        }
    }
}

data class IdentifierState(
    val locationInfo: LocationInfo? = null,
    val location: LocationData? = null
)