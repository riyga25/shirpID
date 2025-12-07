package com.riyga.identifier.presentation.ui.detection_result

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riyga.identifier.data.birds.RecordRepository
import com.riyga.identifier.data.models.LocationData
import com.riyga.identifier.data.models.Record
import com.riyga.identifier.presentation.models.LocationInfo
import com.riyga.identifier.utils.toStringLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class DetectedBird(
    val name: String,
    val confidence: Float
)

data class BirdDetectionResultState(
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val saveError: String? = null
)

class BirdDetectionResultViewModel(
    private val recordRepository: RecordRepository,
    private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BirdDetectionResultState())
    val uiState: StateFlow<BirdDetectionResultState> = _uiState.asStateFlow()
    
    fun saveRecord(
        detectedBirds: List<DetectedBird>,
        location: LocationData?,
        locationInfo: LocationInfo?,
        audioFilePath: String?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, saveError = null)
            
            try {
                // Load labels to map bird names to indices
                // In a real implementation, this should be cached or loaded once
                val labels = loadLabels()
                
                // Map detected bird names to their indices in the labels list
                val birdIndices = detectedBirds.mapNotNull { detectedBird ->
                    val index = labels.indexOfFirst { it == detectedBird.name }
                    if (index >= 0) index else null
                }
                
                // Create record with bird indices
                val record = Record(
                    birds = detectedBirds,
                    latitude = location?.latitude,
                    longitude = location?.longitude,
                    locationName = locationInfo?.let { "${it.city}, ${it.toStringLocation()}" },
                    audioFilePath = audioFilePath
                )
                
                recordRepository.insertRecord(record)
                
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    isSaved = true
                )
                
                Log.d("BirdDetectionResultViewModel", "Saved record with ${birdIndices.size} birds")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveError = "Failed to save record: ${e.message}"
                )
                Log.e("BirdDetectionResultViewModel", "Error saving record: ${e.message}")
            }
        }
    }
    
    fun clearSaveState() {
        _uiState.value = _uiState.value.copy(
            isSaved = false,
            saveError = null
        )
    }
    
    private fun loadLabels(): List<String> {
        return try {
            context.assets.open("labels_en.txt").bufferedReader().use { reader ->
                reader.readLines()
            }
        } catch (e: Exception) {
            Log.e("BirdDetectionResultViewModel", "Error loading labels: ${e.message}")
            emptyList()
        }
    }
}