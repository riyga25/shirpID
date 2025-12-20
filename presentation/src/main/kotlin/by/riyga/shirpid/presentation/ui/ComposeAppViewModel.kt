package by.riyga.shirpid.presentation.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.riyga.shirpid.data.models.GeoDateInfo
import by.riyga.shirpid.data.models.Language
import by.riyga.shirpid.data.models.LocationData
import by.riyga.shirpid.data.preferences.AppPreferences
import by.riyga.shirpid.presentation.models.IdentifiedBird
import by.riyga.shirpid.presentation.utils.BaseViewModel
import by.riyga.shirpid.presentation.utils.UiEffect
import by.riyga.shirpid.presentation.utils.UiEvent
import by.riyga.shirpid.presentation.utils.UiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class ComposeAppViewModel(
    private val appPreferences: AppPreferences
): BaseViewModel<AppContract.State, AppContract.Effect, AppContract.Event>() {

    override fun createInitialState(): AppContract.State = AppContract.State()

    init {
        checkUserLanguage()
    }

    private fun checkUserLanguage() {
        viewModelScope.launch {
            val storeLanguage = appPreferences.language.first()

            if (storeLanguage == null) {
                val deviceLang = Locale.getDefault().language
                Language.fromCode(deviceLang)?.let {newLang ->
                    appPreferences.setLanguage(newLang)
                    setEffect {
                        AppContract.Effect.LanguageUpdated(newLang.code)
                    }
                }
            }
        }
    }
}

class AppContract {
    @Immutable
    data class State(
        val loading: Boolean = false
    ) : UiState

    sealed class Effect : UiEffect {
        data class LanguageUpdated(val code: String) : Effect()
    }

    sealed class Event : UiEvent
}