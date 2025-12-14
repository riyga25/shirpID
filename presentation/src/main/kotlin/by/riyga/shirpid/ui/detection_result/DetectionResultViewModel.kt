package by.riyga.shirpid.ui.detection_result

import androidx.lifecycle.viewModelScope
import data.LabelsRepository
import data.database.RecordRepository
import data.models.Record
import by.riyga.shirpid.models.IdentifiedBird
import by.riyga.shirpid.utils.BaseViewModel
import by.riyga.shirpid.utils.UiEffect
import by.riyga.shirpid.utils.UiEvent
import by.riyga.shirpid.utils.UiState
import kotlinx.coroutines.launch

class DetectionResultViewModel(
    private val recordId: Long,
    private val recordRepository: RecordRepository,
    private val labelsRepository: LabelsRepository
) : BaseViewModel<DetectionResultContract.State, DetectionResultContract.Effect, DetectionResultContract.Event>() {

    override fun createInitialState(): DetectionResultContract.State =
        DetectionResultContract.State()

    init {
        viewModelScope.launch {
            setState { copy(loading = true) }
            val record = recordRepository.getRecordByTimestamp(recordId)

            if (record != null) {
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

    override fun handleEvent(event: DetectionResultContract.Event) {
        super.handleEvent(event)
        when(event) {
            is DetectionResultContract.Event -> {
                viewModelScope.launch {
                    currentState.record?.timestamp?.let {
                        recordRepository.deleteRecordByTimestamp(it)
                    }
                }
                setEffect { DetectionResultContract.Effect.RecordRemoved }
            }
        }
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
        object RecordRemoved: Effect()
    }

    sealed class Event : UiEvent {
        object RemoveRecord: Event()
    }
}