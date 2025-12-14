package by.riyga.shirpid.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.database.RecordRepository
import data.models.Record
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BirdHistoryViewModel(
    private val recordRepository: RecordRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BirdHistoryState())
    val uiState: StateFlow<BirdHistoryState> = _uiState.asStateFlow()
    
    init {
        loadRecords()
        loadStatistics()
    }
    
    private fun loadRecords() {
        viewModelScope.launch {
            recordRepository.getAllRecords().collect { records ->
                _uiState.value = _uiState.value.copy(
                    records = records,
                    isLoading = false
                )
            }
        }
    }
    
    private fun loadStatistics() {
        viewModelScope.launch {
            recordRepository.getAllRecords().collect { records ->
                val uniqueSpeciesCount = records.flatMap { it.birds }.distinct().size
                
                _uiState.value = _uiState.value.copy(
                    totalRecords = records.size,
                    uniqueSpecies = uniqueSpeciesCount
                )
            }
        }
    }
    
    fun deleteAllRecords() {
        viewModelScope.launch {
            recordRepository.deleteAllRecords()
        }
    }
}

data class BirdHistoryState(
    val records: List<Record> = emptyList(),
    val totalRecords: Int = 0,
    val uniqueSpecies: Int = 0,
    val isLoading: Boolean = true
)