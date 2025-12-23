package by.riyga.shirpid.presentation.ui.progress

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import by.riyga.shirpid.data.LabelsRepository
import by.riyga.shirpid.data.database.RecordRepository
import by.riyga.shirpid.data.location.LocationRepository
import by.riyga.shirpid.data.models.DetectedBird
import by.riyga.shirpid.data.models.LocationData
import by.riyga.shirpid.data.models.Record
import by.riyga.shirpid.data.network.GeocoderDataSource
import by.riyga.shirpid.data.preferences.AppPreferences
import by.riyga.shirpid.presentation.models.IdentifiedBird
import by.riyga.shirpid.data.models.GeoDateInfo
import by.riyga.shirpid.presentation.utils.BaseViewModel
import by.riyga.shirpid.presentation.utils.SoundClassifier
import by.riyga.shirpid.presentation.utils.UiEffect
import by.riyga.shirpid.presentation.utils.UiEvent
import by.riyga.shirpid.presentation.utils.UiState
import by.riyga.shirpid.presentation.utils.getAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.cos

class ProgressViewModel(
    private val geocoderDataSource: GeocoderDataSource,
    private val locationRepository: LocationRepository,
    private val recordRepository: RecordRepository,
    private val labelsRepository: LabelsRepository,
    private val appPreferences: AppPreferences
) : BaseViewModel<ProgressContract.State, ProgressContract.Effect, ProgressContract.Event>() {

    override fun createInitialState(): ProgressContract.State = ProgressContract.State()

    private val _timer = MutableStateFlow(0L)
    val timer: StateFlow<Long> = _timer.asStateFlow()

    private var timerJob: Job? = null

    private var detectionSensitivity: Int = 30

    init {
        getServiceOptions()
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
        val currentList = uiState.value.birds.toMutableMap()
        val existing = currentList[birdIndex]

        if (existing == null || confidence > existing.confidence) {
            currentList[birdIndex] = IdentifiedBird(
                index = birdIndex,
                name = labelsRepository.getLabel(birdIndex),
                confidence = confidence
            )

            setState {
                copy(birds = currentList.toMap())
            }

            if (existing == null) {
                setEffect { ProgressContract.Effect.NotifyByHaptic() }
            }
        }

        highlightCurrent(birdIndex)
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
                    latitude = currentState.options.latitude.toDouble(),
                    longitude = currentState.options.longitude.toDouble(),
                    locationName = currentState.geoDateInfo?.getAddress(),
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

    private fun getServiceOptions() {
        viewModelScope.launch {
            setState { copy(loading = true) }
            val location = withContext(Dispatchers.IO) {
                locationRepository.getCurrentLocation()
            }
            val detectionSensitivity = appPreferences.detectionSensitivity.first()
            val useCurrentWeek = appPreferences.useCurrentWeek.first()

            val week = if (useCurrentWeek) {
                val dayOfYear = LocalDate.now().dayOfYear
                (cos(Math.toRadians(dayOfYear * 7.5)) + 1.0).toFloat()
            } else {
                -1F
            }
            
            setState {
                copy(
                    options = SoundClassifier.Options(
                        confidenceThreshold = detectionSensitivity.toFloat() / 100,
                        latitude = location?.latitude?.toFloat() ?: -1F,
                        longitude = location?.longitude?.toFloat() ?: -1F,
                        week = week
                    ),
                    loading = false
                )
            }

            if (location != null) {
                getGeocodeLocation(location)
            }
        }
    }

    private fun getGeocodeLocation(location: LocationData) {
        viewModelScope.launch {
            try {
                val geoDateInfo = geocoderDataSource.getLocationInfo(
                    location.latitude,
                    location.longitude
                )
                setState { copy(geoDateInfo = geoDateInfo) }
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
        val geoDateInfo: GeoDateInfo? = null,
        val savingRecord: Boolean = false,
        val saveError: String? = null,
        val birds: Map<Int, IdentifiedBird> = emptyMap(),
        val currentlyHeardBirds: Set<Int> = emptySet(),
        val options: SoundClassifier.Options = SoundClassifier.Options()
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