package by.riyga.shirpid.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocationIQAddress(
    val city: String? = null,
    val town: String? = null,
    val village: String? = null,
    val municipality: String? = null,
    val road: String? = null,
    val suburb: String? = null,
    val viewpoint: String? = null,
    val region: String? = null,
    val state: String? = null,
    val county: String? = null,
    val postcode: String? = null,
    val country: String,
    @SerialName("country_code")
    val countryCode: String,
    @SerialName("house_number")
    val houseNumber: String? = null
)