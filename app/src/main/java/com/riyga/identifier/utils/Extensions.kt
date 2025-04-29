package com.riyga.identifier.utils

import com.riyga.identifier.data.models.LocationData
import com.riyga.identifier.presentation.models.LocationInfo

fun LocationInfo?.toStringLocation(): String? {
    return this?.city
}

fun LocationData?.toStringLocation(): String? {
    return if (this != null) {
        "${this.latitude}, ${this.longitude}"
    } else null
}