package by.riyga.shirpid.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.riyga.shirpid.data.models.Language
import by.riyga.shirpid.data.preferences.AppPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val appPreferences: AppPreferences
) : ViewModel() {

    val currentLanguage: StateFlow<Language?> = appPreferences.language
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
        
    val detectionSensitivity: StateFlow<Int> = appPreferences.detectionSensitivity
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 30
        )
        
    val useCurrentWeek: StateFlow<Boolean> = appPreferences.useCurrentWeek
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun setLanguage(language: Language) {
        viewModelScope.launch {
            appPreferences.setLanguage(language)
        }
    }
    
    fun setDetectionSensitivity(sensitivity: Int) {
        viewModelScope.launch {
            appPreferences.setDetectionSensitivity(sensitivity)
        }
    }
    
    fun setUseCurrentWeek(value: Boolean) {
        viewModelScope.launch {
            appPreferences.setUseCurrentWeek(value)
        }
    }
}