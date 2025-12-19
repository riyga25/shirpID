package by.riyga.shirpid.data.models

import kotlinx.serialization.Serializable

@Serializable
data class GeoDateInfo(
    val latitude: Double,
    val longitude: Double,
    val city: String,
    val street: String,
    val houseNumber: String,
    val displayName: String,
    val postcode: String,
    val country: String,
    val countryCode: String,
    val state: String
)