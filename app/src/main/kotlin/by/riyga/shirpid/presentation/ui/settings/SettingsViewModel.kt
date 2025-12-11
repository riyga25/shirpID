package by.riyga.shirpid.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.riyga.shirpid.data.models.Language
import by.riyga.shirpid.data.preferences.AppPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val appPreferences: AppPreferences
) : ViewModel() {

    val currentLanguage: StateFlow<Language> = appPreferences.language
        .map { Language.fromCode(it) }
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