package by.riyga.shirpid.data.models

import by.riyga.shirpid.presentation.models.LocationInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocationIQResponse(
    @SerialName("address")
    val address: LocationIQAddress,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("lat")
    val lat: String,
    @SerialName("lon")
    val lon: String,
    @SerialName("place_id")
    val placeId: String
) {
    fun toDomain(): LocationInfo {
        return LocationInfo(
            latitude = lat.toDoubleOrNull() ?: 0.0,
            longitude = lon.toDoubleOrNull() ?: 0.0,
            city = address.city ?: address.town ?: address.village ?: address.municipality ?: "",
            street = address.road ?: "",
            houseNumber = address.houseNumber ?: "",
            fullAddress = displayName,
            postcode = address.postcode ?: "",
            country = address.country,
            countryCode = address.countryCode
        )
    }
}