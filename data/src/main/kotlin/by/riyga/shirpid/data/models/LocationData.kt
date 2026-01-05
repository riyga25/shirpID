package by.riyga.shirpid.data.models

import kotlinx.serialization.Serializable

@Serializable
data class LocationData(
    val location: LatLon,
    val accuracy: Float? = null,
    val timestamp: Long? = null
)