package by.riyga.shirpid.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import kotlinx.serialization.Serializable
import retrofit2.Converter

@Entity(tableName = "records")
@Serializable
data class Record(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long,
    @TypeConverters(Converter::class)
    val birds: Map<Int, List<DetectedBird>>,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val audioFilePath: String,
    val chunkDuration: Int
)