package data.models

import kotlinx.serialization.Serializable

@Serializable
data class DetectedBird(
    val index: Int,
    val confidence: Float
)