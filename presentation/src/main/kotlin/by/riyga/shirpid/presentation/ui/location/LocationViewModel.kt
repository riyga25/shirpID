package by.riyga.shirpid.presentation.ui.location

import androidx.lifecycle.viewModelScope
import by.riyga.shirpid.data.location.LocationRepository
import by.riyga.shirpid.data.models.LocationData
import by.riyga.shirpid.presentation.utils.BaseViewModel
import by.riyga.shirpid.presentation.utils.UiEffect
import by.riyga.shirpid.presentation.utils.UiEvent
import by.riyga.shirpid.presentation.utils.UiState
import kotlinx.coroutines.launch

class LocationViewModel(
    private val locationRepository: LocationRepository
) : BaseViewModel<LocationContract.State, LocationContract.Effect, LocationContract.Event>() {

    override fun createInitialState(): LocationContract.State = LocationContract.State()

    init {
        getCurrentLocation()
    }

    private fun getCurrentLocation() {
        viewModelScope.launch {
            setState { copy(loading = true) }
            val location = locationRepository.getCurrentLocation()
            setState { copy(loading = false, location = location) }
        }
    }
}

class LocationContract {
    data class State(
        val loading: Boolean = false,
        val location: LocationData? = null
    ) : UiState

    sealed class Effect : UiEffect

    sealed class Event : UiEvent
}