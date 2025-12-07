package com.riyga.identifier.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.riyga.identifier.presentation.ui.DetectedBird
import kotlinx.serialization.Serializable

@Entity(tableName = "records")
@Serializable
data class Record(
    @PrimaryKey
    val timestamp: Long = System.currentTimeMillis(),
    val birds: List<DetectedBird>,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val audioFilePath: String? = null
)