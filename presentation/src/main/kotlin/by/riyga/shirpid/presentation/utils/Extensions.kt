package by.riyga.shirpid.presentation.utils

import android.app.LocaleManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.LocaleList
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Instant

fun GeoDateInfo?.getAddress(): String? {
    return this?.displayName?.takeUntilComma(4)
}

fun LocationData?.toStringLocation(): String? {
    return if (this != null) {
        "${String.format("%.4f", this.location.latitude)}, ${String.format("%.4f", this.location.longitude)}"
    } else null
}

fun <T> MutableStateFlow<T>.setState(reduce: T.() -> T) {
    val newState = this.value.reduce()
    this.value = newState
}

fun Context.isAudioExists(uri: String?): Boolean {
    if (uri == null) return false

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

fun Context.updateAppLocale(languageCode: String) {
    val locale = Locale(languageCode)
    Locale.setDefault(locale)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeManager = this.getSystemService(Context.LOCALE_SERVICE) as LocaleManager
        localeManager.applicationLocales = LocaleList(locale)
    } else {
        @Suppress("DEPRECATION")
        val config = this.resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        this.resources.updateConfiguration(config, this.resources.displayMetrics)
    }
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

fun Instant.formatToString(
    pattern: String,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): String {
    val dateFormat = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
    val ldt = this.toLocalDateTime(timeZone).toJavaLocalDateTime()
    return dateFormat.format(ldt)
}