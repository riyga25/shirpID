package by.riyga.shirpid.ui.progress

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import data.LabelsRepository
import data.database.RecordRepository
import data.location.LocationRepository
import data.models.DetectedBird
import data.models.LocationData
import data.models.Record
import data.network.GeocoderDataSource
import by.riyga.shirpid.models.IdentifiedBird
import data.models.LocationInfo
import by.riyga.shirpid.utils.BaseViewModel
import by.riyga.shirpid.utils.UiEffect
import by.riyga.shirpid.utils.UiEvent
import by.riyga.shirpid.utils.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ProgressViewModel(
    private val geocoderDataSource: GeocoderDataSource,
    private val locationRepository: LocationRepository,
    private val recordRepository: RecordRepository,
    private val labelsRepository: LabelsRepository
) : BaseViewModel<ProgressContract.State, ProgressContract.Effect, ProgressContract.Event>() {

    override fun createInitialState(): ProgressContract.State = ProgressContract.State()

    private val _timer = MutableStateFlow(0L)
    val timer: StateFlow<Long> = _timer.asStateFlow()

    private var timerJob: Job? = null

    init {
        fetchLocation()
    }

    override fun handleEvent(event: ProgressContract.Event) {
        super.handleEvent(event)
        when (event) {
            is ProgressContract.Event.SaveRecord -> {
                saveRecord(event.audioPath)
            }

            is ProgressContract.Event.AddIdentification -> {
                addIdentifiedBird(event.birdIndex, event.confidence)
            }

            is ProgressContract.Event.StartTimer -> {
                startTimer()
            }

            is ProgressContract.Event.StopTimer -> {
                stopTimer()
            }
        }
    }

    private fun addIdentifiedBird(birdIndex: Int, confidence: Float) {
        if (confidence * 100 > 30) {
            val currentList = mutableMapOf<Int, IdentifiedBird>()
                .apply {
                    putAll(uiState.value.birds)
                }

            if (birdIndex !in currentList) {
                currentList[birdIndex] = IdentifiedBird(
                    index = birdIndex,
                    name = labelsRepository.getLabel(birdIndex),
                    confidence = confidence
                )

                setState {
                    copy(birds = currentList.toMap())
                }.run {
                    setEffect { ProgressContract.Effect.NotifyByHaptic() }
                }
            }
            highlightCurrent(birdIndex)
        } else {
            println("ProgressScreen: less than 30% -> $confidence = $birdIndex")
        }
    }

    private fun highlightCurrent(birdIndex: Int) {
        setState {
            copy(currentlyHeardBirds = currentState.currentlyHeardBirds + birdIndex)
        }

        viewModelScope.launch {
            delay(1000)
            setState {
                copy(
                    currentlyHeardBirds = currentState.currentlyHeardBirds
                        .filterNot { it == birdIndex }
                        .toSet())
            }
        }
    }

    private fun saveRecord(
        audioPath: String
    ) {
        viewModelScope.launch {
            setState {
                copy(savingRecord = true, saveError = null)
            }

            try {
                val record = Record(
                    birds = currentState.birds.values.map {
                        DetectedBird(it.index, it.confidence)
                    }.toList(),
                    latitude = currentState.location?.latitude,
                    longitude = currentState.location?.longitude,
                    locationName = currentState.locationInfo?.let { "${it.city}, ${it.country}" },
                    audioFilePath = audioPath
                )

                val id = recordRepository.insertRecord(record)
                setEffect {
                    ProgressContract.Effect.ShowResult(id)
                }
            } catch (e: Exception) {
                setState {
                    copy(
                        savingRecord = false,
                        saveError = "Save record error: ${e.message}"
                    )
                }
                Log.e("ProgressViewModel", "Error saving record: ${e.message}")
            }
        }
    }

    private fun fetchLocation() {
        viewModelScope.launch {
            setState { copy(loading = true) }
            val loc = locationRepository.getCurrentLocation()
            setState { copy(location = loc, loading = false) }

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
                setState { copy(locationInfo = locationInfo) }
            } catch (err: Throwable) {
                Log.e("APP", err.localizedMessage ?: "Unknown error")
            }
        }
    }

    private fun startTimer() {
        if (timerJob != null) return   // защита от повторного запуска

        val startTime = System.currentTimeMillis()

        timerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime

                _timer.value = elapsed

                delay(100)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }
}

class ProgressContract {
    @Immutable
    data class State(
        val loading: Boolean = false,
        val locationInfo: LocationInfo? = null,
        val location: LocationData? = null,
        val savingRecord: Boolean = false,
        val saveError: String? = null,
        val birds: Map<Int, IdentifiedBird> = emptyMap(),
        val currentlyHeardBirds: Set<Int> = emptySet()
    ) : UiState

    sealed class Effect : UiEffect {
        class NotifyByHaptic : Effect()
        data class ShowResult(val id: Long) : Effect()
    }

    sealed class Event : UiEvent {
        data class SaveRecord(
            val audioPath: String
        ) : Event()

        data class AddIdentification(
            val birdIndex: Int,
            val confidence: Float
        ) : Event()

        class StartTimer : Event()
        class StopTimer : Event()
    }
}