package by.riyga.shirpid.presentation.utils

import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Compose-specific extension functions
 */
object ComposeExtensions {
    /**
     * Gets color based on confidence value (0.0-1.0)
     */
    @Composable
    fun getConfidenceColor(confidence: Float): Color {
        val badColor = MaterialTheme.colorScheme.errorContainer
        val middleColor = MaterialTheme.colorScheme.secondaryContainer
        val goodColor = MaterialTheme.colorScheme.primaryContainer

        val c = confidence.coerceIn(0f, 1f)

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

    /**
     * Helper function for updating StateFlow with reducer
     */
    fun <T> setState(flow: MutableStateFlow<T>, reduce: T.() -> T) {
        val newState = flow.value.reduce()
        flow.value = newState
    }
}

/**
 * Extension function for Float to get confidence color
 */
@Composable
fun Float.getConfidenceColor(): Color {
    return ComposeExtensions.getConfidenceColor(this)
}

/**
 * Extension function for MutableStateFlow to set state
 */
fun <T> MutableStateFlow<T>.setState(reduce: T.() -> T) {
    ComposeExtensions.setState(this, reduce)
}
