package by.riyga.shirpid.presentation.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Instant

fun formatSeconds(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}

fun formatSecondsRange(startSeconds: Int, endSeconds: Int): String {
    val startTime = formatSeconds(startSeconds)
    val endTime = formatSeconds(endSeconds)
    return "$startTime - $endTime"
}

fun getWeek(date: Instant): Float {
    val localDate = date.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val dayOfMonth = localDate.day
    val weekInMonth = (dayOfMonth - 1) / 7 + 1
    val month = localDate.month.number - 1
    val weekNumber48 = month * 4 + weekInMonth
    return weekNumber48.toFloat()
}

fun formatProgressTime(elapsedMs: Long): String {
    val totalSeconds = elapsedMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

// Утилитарная функция для проверки разрешений
fun isPermissionGranted(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        permission
    ) == PackageManager.PERMISSION_GRANTED
}

fun share(
    context: Context,
    subject: Uri,
    chooserText: String?
) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, subject)
        type = "audio/x-wav"
    }
    val shareIntent = Intent.createChooser(sendIntent, chooserText)
    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    ContextCompat.startActivity(context, shareIntent, null)
}