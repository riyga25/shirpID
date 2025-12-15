package by.riyga.shirpid.ui.detection_result

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewModelScope
import data.LabelsRepository
import data.database.RecordRepository
import data.models.Record
import by.riyga.shirpid.models.IdentifiedBird
import by.riyga.shirpid.player.AudioPlayer
import by.riyga.shirpid.player.PlayerState
import by.riyga.shirpid.utils.BaseViewModel
import by.riyga.shirpid.utils.UiEffect
import by.riyga.shirpid.utils.UiEvent
import by.riyga.shirpid.utils.UiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DetectionResultViewModel(
    private val recordId: Long,
    private val recordRepository: RecordRepository,
    private val labelsRepository: LabelsRepository,
    private val audioPlayer: AudioPlayer
) : BaseViewModel<DetectionResultContract.State, DetectionResultContract.Effect, DetectionResultContract.Event>() {

    override fun createInitialState(): DetectionResultContract.State =
        DetectionResultContract.State()

    val mediaState: StateFlow<PlayerState> = audioPlayer.playerState

    init {
        getRecord()
    }

    override fun handleEvent(event: DetectionResultContract.Event) {
        super.handleEvent(event)
        when(event) {
            is DetectionResultContract.Event.RemoveRecord -> {
                viewModelScope.launch {
                    currentState.record?.timestamp?.let {
                        recordRepository.deleteRecordByTimestamp(it)
                    }
                }
                currentState.record?.let {
                    setEffect { DetectionResultContract.Effect.RecordRemoved(it.audioFilePath) }
                }
            }
        }
    }

    fun prepareAudio(uri: String) {
        audioPlayer.prepare(uri)
    }

    fun pauseAudio() {
        audioPlayer.pause()
    }

    fun playAudio() {
        audioPlayer.start()
    }

    private fun getRecord() {
        viewModelScope.launch {
            setState { copy(loading = true) }
            val record = recordRepository.getRecordByTimestamp(recordId)

            if (record != null) {
                prepareAudio(record.audioFilePath)
                setState {
                    copy(
                        record = record,
                        loading = false,
                        birds = record.birds.map {
                            IdentifiedBird(
                                index = it.index,
                                name = labelsRepository.getLabel(it.index),
                                confidence = it.confidence
                            )
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

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}


class DetectionResultContract {
    data class State(
        val loading: Boolean = false,
        val error: Boolean = false,
        val record: Record? = null,
        val birds: List<IdentifiedBird> = emptyList()
    ) : UiState

    sealed class Effect : UiEffect {
        data class RecordRemoved(val uri: String): Effect()
    }

    sealed class Event : UiEvent {
        object RemoveRecord: Event()
    }
}