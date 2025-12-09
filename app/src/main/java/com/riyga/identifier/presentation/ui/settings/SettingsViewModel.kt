package com.riyga.identifier.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riyga.identifier.data.models.Language
import com.riyga.identifier.data.preferences.AppPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val appPreferences: AppPreferences
) : ViewModel() {

    val currentLanguage: StateFlow<String> = appPreferences.language
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppPreferences.DEFAULT_LANGUAGE
        )

    fun setLanguage(language: Language) {
        viewModelScope.launch {
            appPreferences.setLanguage(language)
        }
    }
}