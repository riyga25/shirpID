package by.riyga.shirpid.presentation.utils

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.core.content.ContextCompat
import by.riyga.shirpid.data.models.LocationData
import by.riyga.shirpid.data.models.GeoDateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.core.net.toUri
import java.util.Locale

fun GeoDateInfo?.getAddress(): String? {
    return this?.displayName?.takeUntilComma(4)
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

@Composable
fun Float.getConfidenceColor(): Color {
    val badColor = MaterialTheme.colorScheme.errorContainer
    val middleColor = MaterialTheme.colorScheme.secondaryContainer
    val goodColor = MaterialTheme.colorScheme.primaryContainer

    val c = this.coerceIn(0f, 1f)

    val targetColor = when {
        c < 0.5f -> {
            lerp(
                badColor, // 0%
                middleColor, // 50%
                c / 0.5f
            )
        }
        else -> {
            lerp(
                middleColor, // 50%
                goodColor, // 100%
                (c - 0.5f) / 0.5f
            )
        }
    }

    return animateColorAsState(
        targetValue = targetColor,
        label = "confidenceColor"
    ).value
}

fun Float.toPercentString(decimals: Int = 1): String {
    return String.format(Locale.US, "%.${decimals}f%%", this * 100)
}

private fun String.takeUntilComma(maxCommas: Int): String {
    var count = 0

    for (i in indices) {
        if (this[i] == ',') {
            count++
            if (count == maxCommas) {
                return substring(0, i)
            }
        }
    }
    return this
}