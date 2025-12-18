package by.riyga.shirpid.presentation.utils

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import by.riyga.shirpid.data.models.LocationData
import by.riyga.shirpid.data.models.LocationInfo
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.core.net.toUri

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
    return ContextCompat.checkSelfPermission(
        context,
        permission
    ) == PackageManager.PERMISSION_GRANTED
}

fun <T> MutableStateFlow<T>.setState(reduce: T.() -> T) {
    val newState = this.value.reduce()
    this.value = newState
}

fun Context.isAudioExists(uri: String): Boolean {
    val projection = arrayOf(MediaStore.Audio.Media._ID)

    return try {
        contentResolver.query(
            uri.toUri(),
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            cursor.count > 0
        } ?: false
    } catch (e: Exception) {
        println("isAudioExists error ${e.message}")
        false
    }
}

fun Context.deleteAudio(uri: Uri): Boolean {
    return try {
        contentResolver.delete(uri, null, null) > 0
    } catch (e: SecurityException) {
        false
    }
}