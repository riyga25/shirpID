package com.riyga.identifier.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
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

fun isPermissionGranted(
    context: Context,
    permission: String
): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        permission
    ) == PackageManager.PERMISSION_GRANTED
}