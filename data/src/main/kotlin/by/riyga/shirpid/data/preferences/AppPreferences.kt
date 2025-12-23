package by.riyga.shirpid.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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
        val DETECTION_SENSITIVITY_KEY = intPreferencesKey("detection_sensitivity")
        val USE_CURRENT_WEEK = booleanPreferencesKey("use_current_week")
    }

    val language: Flow<Language?> = context.dataStore.data
        .map { preferences ->
            Language.fromCode(preferences[LANGUAGE_KEY])
        }.distinctUntilChanged()

    val detectionSensitivity: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[DETECTION_SENSITIVITY_KEY] ?: 30 // Default value is 30
        }.distinctUntilChanged()

    val useCurrentWeek: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[USE_CURRENT_WEEK] ?: true
        }.distinctUntilChanged()

    suspend fun setLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = languageCode
        }
    }
    
    suspend fun setLanguage(language: Language) {
        setLanguage(language.code)
    }
    
    suspend fun setDetectionSensitivity(sensitivity: Int) {
        context.dataStore.edit { preferences ->
            preferences[DETECTION_SENSITIVITY_KEY] = sensitivity
        }
    }
    
    suspend fun setUseCurrentWeek(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_CURRENT_WEEK] = value
        }
    }
}