package by.riyga.shirpid.presentation.utils

import by.riyga.shirpid.data.models.GeoDateInfo
import by.riyga.shirpid.data.models.LocationData
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Utility functions for working with location data
 */
object LocationUtils {
    /**
     * Gets formatted address from GeoDateInfo
     */
    fun getAddress(geoDateInfo: GeoDateInfo?): String? {
        return geoDateInfo?.displayName?.takeUntilComma(4)
    }

    /**
     * Converts LocationData to formatted string
     */
    fun toStringLocation(locationData: LocationData?): String? {
        return if (locationData != null) {
            "${String.format("%.4f", locationData.location.latitude)}, ${String.format("%.4f", locationData.location.longitude)}"
        } else null
    }

    /**
     * Calculates week number (1-48) from Instant
     */
    fun getWeek(date: Instant): Float {
        val localDate = date.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val dayOfMonth = localDate.day
        val weekInMonth = (dayOfMonth - 1) / 7 + 1
        val month = localDate.month.number - 1
        val weekNumber48 = month * 4 + weekInMonth
        return weekNumber48.toFloat()
    }
}

/**
 * Extension function for GeoDateInfo to get address
 */
fun GeoDateInfo?.getAddress(): String? {
    return LocationUtils.getAddress(this)
}

/**
 * Extension function for LocationData to string
 */
fun LocationData?.toStringLocation(): String? {
    return LocationUtils.toStringLocation(this)
}
