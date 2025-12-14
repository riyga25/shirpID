package data.database.converters

import androidx.room.TypeConverter
import data.models.DetectedBird
import kotlinx.serialization.json.Json

class Converters {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @TypeConverter
    fun fromDetectedBirds(list: List<DetectedBird>): String {
        return json.encodeToString(list)
    }

    @TypeConverter
    fun toDetectedBirds(data: String): List<DetectedBird> {
        return if (data.isBlank()) {
            emptyList()
        } else {
            json.decodeFromString(data)
        }
    }
}