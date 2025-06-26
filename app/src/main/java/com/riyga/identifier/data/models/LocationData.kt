package com.riyga.identifier.data.models

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val timestamp: Long? = null
)