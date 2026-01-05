package by.riyga.shirpid.data.models

import kotlinx.serialization.Serializable

@Serializable
data class LatLon(
    val latitude: Double,
    val longitude: Double
)