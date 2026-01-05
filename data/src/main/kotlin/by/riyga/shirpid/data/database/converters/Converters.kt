package by.riyga.shirpid.data.database.converters

import androidx.room.TypeConverter
import by.riyga.shirpid.data.models.DetectedBird
import kotlinx.serialization.json.Json

class Converters {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @TypeConverter
    fun fromMap(map: Map<Int, List<DetectedBird>>): String {
        return json.encodeToString(map)
    }

    @TypeConverter
    fun toMap(jsonString: String): Map<Int, List<DetectedBird>> {
        return json.decodeFromString(jsonString)
    }
}