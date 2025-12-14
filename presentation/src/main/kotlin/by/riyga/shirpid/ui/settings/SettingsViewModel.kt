package by.riyga.shirpid.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.models.Language
import data.preferences.AppPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val appPreferences: AppPreferences
) : ViewModel() {

    val currentLanguage: StateFlow<Language> = appPreferences.language
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Language.fromCode(AppPreferences.DEFAULT_LANGUAGE)
        )

    fun setLanguage(language: Language) {
        viewModelScope.launch {
            appPreferences.setLanguage(language)
        }
    }
}