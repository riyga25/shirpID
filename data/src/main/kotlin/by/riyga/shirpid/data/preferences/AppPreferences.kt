package by.riyga.shirpid.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import by.riyga.shirpid.data.models.Language
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

// At the top level of your kotlin file:
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class AppPreferences(private val context: Context) {
    companion object {
        val LANGUAGE_KEY = stringPreferencesKey("language")
        val DEFAULT_LANGUAGE = Language.ENGLISH.code
    }

    val language: Flow<Language> = context.dataStore.data
        .map { preferences ->
            Language.fromCode(preferences[LANGUAGE_KEY] ?: DEFAULT_LANGUAGE)
        }.distinctUntilChanged()

    suspend fun setLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = languageCode
        }
    }
    
    suspend fun setLanguage(language: Language) {
        setLanguage(language.code)
    }
}