package by.riyga.shirpid.presentation.utils

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Instant

/**
 * Utility functions for formatting dates and times
 */
object DateFormatter {
    /**
     * Formats seconds to MM:SS format
     */
    fun formatSeconds(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }

    /**
     * Formats a range of seconds to "MM:SS - MM:SS" format
     */
    fun formatSecondsRange(startSeconds: Int, endSeconds: Int): String {
        val startTime = formatSeconds(startSeconds)
        val endTime = formatSeconds(endSeconds)
        return "$startTime - $endTime"
    }

    /**
     * Formats elapsed milliseconds to MM:SS format
     */
    fun formatProgressTime(elapsedMs: Long): String {
        val totalSeconds = elapsedMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    /**
     * Formats milliseconds to MM:SS format
     */
    fun formatTime(millis: Long?): String {
        if (millis == null) return ""

        val timeMin = millis / 60000
        val timeSec = (millis / 1000) % 60

        val minString = if (timeMin < 10) "0$timeMin" else "$timeMin"
        val secString = if (timeSec < 10) "0$timeSec" else "$timeSec"

        return "$minString:$secString"
    }

}

/**
 * Extension function for Instant to format to string
 */
fun Instant.formatToString(
    pattern: String,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): String {
    val dateFormat = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
    val ldt = this.toLocalDateTime(timeZone).toJavaLocalDateTime()
    return dateFormat.format(ldt)
}
