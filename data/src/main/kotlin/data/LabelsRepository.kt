package data

import android.content.Context
import android.util.Log
import data.models.Language
import data.preferences.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface LabelsRepository {
    fun getLabel(index: Int): String
}

class LabelsRepositoryImpl(
    private val context: Context,
    private val appPreferences: AppPreferences,
    externalScope: CoroutineScope
): LabelsRepository {

    @Volatile
    private var labels : List<String> = emptyList()

    init {
        externalScope.launch {
            appPreferences.language.collect { lang ->
                loadLabels(lang)
            }
        }
    }

    override fun getLabel(index: Int): String {
        return labels.getOrNull(index) ?: "-"
    }

    private fun loadLabels(language: Language) {
        labels = try {
            context.assets.open(language.assets).bufferedReader().use { reader ->
                reader.readLines()
            }
        } catch (e: Exception) {
            Log.e("LabelsRepository", "Error loading labels: ${e.message}")
            emptyList()
        }
    }
}