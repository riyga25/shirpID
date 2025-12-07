package com.riyga.identifier.data.database

import androidx.room.TypeConverter
import com.riyga.identifier.presentation.ui.DetectedBird
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
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