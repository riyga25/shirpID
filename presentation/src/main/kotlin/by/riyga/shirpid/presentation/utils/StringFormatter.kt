package by.riyga.shirpid.presentation.utils

import java.util.Locale

/**
 * Utility functions for formatting strings
 */
object StringFormatter {
    /**
     * Converts float to percent string
     */
    fun toPercentString(value: Float, decimals: Int = 1): String {
        return String.format(Locale.US, "%.${decimals}f%%", value * 100)
    }
}

/**
 * Extension function for Float to percent string
 */
fun Float.toPercentString(decimals: Int = 1): String {
    return StringFormatter.toPercentString(this, decimals)
}

/**
 * Takes string until specified number of commas
 */
fun String.takeUntilComma(maxCommas: Int): String {
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
