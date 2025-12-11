package by.riyga.shirpid.presentation.models

import kotlinx.serialization.Serializable

@Serializable
data class LocationInfo(
    val latitude: Double,
    val longitude: Double,
    val city: String,
    val street: String,
    val houseNumber: String,
    val fullAddress: String,
    val postcode: String,
    val country: String,
    val countryCode: String
)