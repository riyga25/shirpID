package by.riyga.shirpid.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import data.models.LocationData
import data.models.LocationInfo

fun LocationInfo?.toStringLocation(): String? {
    return this?.city
}

fun LocationData?.toStringLocation(): String? {
    return if (this != null) {
        "${this.latitude}, ${this.longitude}"
    } else null
}

// Утилитарная функция для проверки разрешений
fun isPermissionGranted(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}