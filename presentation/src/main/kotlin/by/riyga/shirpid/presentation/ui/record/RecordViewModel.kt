package by.riyga.shirpid.presentation.ui.record

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import by.riyga.shirpid.data.LabelsRepository
import by.riyga.shirpid.data.database.RecordRepository
import by.riyga.shirpid.data.location.LocationRepository
import by.riyga.shirpid.data.models.DetectedBird
import by.riyga.shirpid.data.models.LatLon
import by.riyga.shirpid.data.models.Record
import by.riyga.shirpid.data.preferences.AppPreferences
import by.riyga.shirpid.presentation.models.IdentifiedBird
import by.riyga.shirpid.presentation.player.AudioPlayer
import by.riyga.shirpid.presentation.player.PlayerState
import by.riyga.shirpid.presentation.utils.BaseViewModel
import by.riyga.shirpid.presentation.utils.SoundClassifier
import by.riyga.shirpid.presentation.utils.UiEffect
import by.riyga.shirpid.presentation.utils.UiEvent
import by.riyga.shirpid.presentation.utils.UiState
import by.riyga.shirpid.presentation.utils.WavManager
import by.riyga.shirpid.presentation.utils.LocationUtils
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

class RecordViewModel(
    private val recordRepository: RecordRepository,
    private val labelsRepository: LabelsRepository,
    private val soundClassifier: SoundClassifier,
    private val audioPlayer: AudioPlayer,
    private val locationRepository: LocationRepository,
    private val wavManager: WavManager,
    private val appPreferences: AppPreferences
) : BaseViewModel<RecordContract.State, RecordContract.Effect, RecordContract.Event>() {

    override fun createInitialState(): RecordContract.State =
        RecordContract.State()

    val mediaState: StateFlow<PlayerState> = audioPlayer.playerState

    private var birdsCache: Map<Int, List<IdentifiedBird>> = emptyMap()

    init {
        viewModelScope.launch {
            val location = locationRepository.getCurrentLocation()?.location
            setState { copy(currentLocation = location) }
        }
    }

    override fun handleEvent(event: RecordContract.Event) {
        super.handleEvent(event)
        when (event) {
            is RecordContract.Event.RemoveRecord -> {
                viewModelScope.launch {
                    currentState.record?.id?.let {
                        recordRepository.deleteRecordById(it)
                    }
                }
                currentState.record?.audioFilePath?.let {
                    setEffect { RecordContract.Effect.RecordRemoved(it) }
                }
            }
        }
    }

    fun prepareAudioPlayer(uri: String) {
        audioPlayer.prepare(uri)
    }

    fun pauseAudio() {
        audioPlayer.pause()
    }

    fun playAudio() {
        audioPlayer.start()
    }

    fun createRecordFromAudioFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            val audioFile = WavManager(context).copyWavToMediaStore(uri) ?: return@launch

            val record = Record(
                timestamp = Clock.System.now().toEpochMilliseconds(),
                birds = emptyMap(),
                latitude = null,
                longitude = null,
                locationName = null,
                audioFilePath = audioFile.toString(),
                chunkDuration = 3000
            )

            recordRepository.insertRecord(record)

            setState {
                copy(
                    fileName = getFileName(context, uri),
                    record = record
                )
            }
            prepareAudioPlayer(uri.toString())
        }
    }

    fun getFileName(context: Context, uri: Uri?): String {
        if (uri == null) return ""

        var fileName = ""
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                fileName = cursor.getString(nameIndex)
            }
        }
        return fileName
    }

    fun getRecordById(context: Context, recordId: Long) {
        viewModelScope.launch {
            setState { copy(loading = true) }
            val record = recordRepository.getRecordById(recordId)

            record?.audioFilePath?.let {
                prepareAudioPlayer(it)
            }

            if (record != null) {
                setState {
                    copy(
                        fileName = getFileName(context, record?.audioFilePath?.toUri()),
                        record = record,
                        loading = false,
                        birds = record.birds.mapValues {
                            it.value.map { bird ->
                                IdentifiedBird(
                                    index = bird.index,
                                    name = labelsRepository.getLabel(bird.index),
                                    confidence = bird.confidence
                                )
                            }
                        }
                    )
                }
            } else {
                setState {
                    copy(
                        error = true,
                        loading = false
                    )
                }
            }
        }
    }

    fun setDate(epochMillis: Long) {
        val tz = TimeZone.currentSystemDefault()
        val selectedDate = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(tz).date
        val currentTime = Instant.fromEpochMilliseconds(currentState.record?.timestamp ?: 0L)
            .toLocalDateTime(tz).time
        val newDateTime = LocalDateTime(selectedDate, currentTime).toInstant(tz)

        currentState.record?.copy(timestamp = newDateTime.toEpochMilliseconds())?.let {
            updateRecord(updatedRecord = it, startClassifier = true)
        }
    }

    fun setLocationFromMap(lat: Double, lon: Double) {
        currentState.record?.copy(
            latitude = lat,
            longitude = lon
        )?.let {
            updateRecord(updatedRecord = it, startClassifier = true)
        }
    }

    fun setDefaultLocation() {
        viewModelScope.launch {
            currentState.record?.copy(
                latitude = currentState.currentLocation?.latitude,
                longitude = currentState.currentLocation?.longitude
            )?.let {
                updateRecord(updatedRecord = it, startClassifier = true)
            }
        }
    }

    private fun updateRecord(updatedRecord: Record, startClassifier: Boolean = false) {
        viewModelScope.launch {
            recordRepository.updateRecord(updatedRecord)
        }

        setState { copy(record = updatedRecord) }

        if (startClassifier) {
            setState { copy(birds = emptyMap()) }
        }

        if (startClassifier) {
            findBirdsInFile()
        }
    }

    private fun findBirdsInFile() {
        val record = currentState.record ?: return
        currentState.record?.latitude ?: return

        viewModelScope.launch {
            val detectionSensitivity = appPreferences.detectionSensitivity.first()
            val options = SoundClassifier.Options(
                latitude = record.latitude?.toFloat() ?: -1F,
                longitude = record.longitude?.toFloat() ?: -1F,
                week = LocationUtils.getWeek(Instant.fromEpochMilliseconds(record.timestamp)),
                confidenceThreshold = detectionSensitivity.toFloat() / 100
            )

            soundClassifier.initializeModels(options)

            record.audioFilePath.let { uri ->
                wavManager.processFileByChunks(
                    uri = uri.toUri(),
                    onFinished = {
                        soundClassifier.releaseModels()
                        setState { copy(classifyProgressPercent = 0, birds = birdsCache) }
                        val upRecord = currentState.record!!
                            .copy(birds = birdsCache.mapValues {
                                it.value.map { bird ->
                                    DetectedBird(
                                        index = bird.index,
                                        confidence = bird.confidence
                                    )
                                }
                            })
                        updateRecord(upRecord)
                        birdsCache = emptyMap()
                    },
                    onUpdateProgress = {
                        setState { copy(classifyProgressPercent = it) }
                    }
                ) { chunkIndex, buffer ->
                    val result = soundClassifier.classify(buffer)
                        .map { it.index to it.value }
                    saveIdentifications(chunkIndex, result)
                }
            }
        }
    }

    private fun saveIdentifications(
        chunk: Int,
        result: List<Pair<Int, Float>>
    ) {
        if (result.isNotEmpty()) {
            val currentList = birdsCache.toMutableMap()
            currentList[chunk] = result.map {
                IdentifiedBird(
                    index = it.first,
                    name = labelsRepository.getLabel(it.first),
                    confidence = it.second
                )
            }
            birdsCache = currentList
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
        soundClassifier.releaseModels()
    }
}

class RecordContract {
    data class State(
        val loading: Boolean = false,
        val error: Boolean = false,
        val fileName: String? = null,
        val classifyProgressPercent: Int = 0,
        val record: Record? = null,
        val birds: Map<Int, List<IdentifiedBird>> = emptyMap(),
        val currentLocation: LatLon? = null
    ) : UiState

    sealed class Effect : UiEffect {
        data class RecordRemoved(val uri: String) : Effect()
    }

    sealed class Event : UiEvent {
        object RemoveRecord : Event()
    }
}