package org.tensorflow.lite.examples.soundclassifier.ui

import android.location.Location
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.soundclassifier.LocationHelper
import org.tensorflow.lite.examples.soundclassifier.SoundClassifier

@Composable
fun ComposeApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val soundClassifier = remember {
        SoundClassifier(
            context = context,
            externalScope = scope
        )
    }
    var screenState by remember { mutableStateOf(Screen.START) }
    var location: Location? by remember { mutableStateOf(null) }
    val locationHelper = remember { LocationHelper(context) }

    MainLayout(
        screenState = screenState,
        onStart = {
            screenState = Screen.PROGRESS
        },
        onRestart = {
            screenState = Screen.START
        },
        soundClassifier = soundClassifier,
        requestLocation = {
            scope.launch {
                locationHelper.getCurrentLocation()?.let {
                    location = it
                    soundClassifier.runMetaInterpreter(it)
                }
            }
        },
        location = location
    )
}

@Composable
private fun MainLayout(
    screenState: Screen,
    soundClassifier: SoundClassifier,
    location: Location? = null,
    onStart: () -> Unit,
    onRestart: () -> Unit,
    requestLocation: () -> Unit = {}
) {
    MaterialTheme {
        when (screenState) {
            Screen.START -> StartScreen(
                requestLocation = requestLocation
            ) {
                onStart()
            }

            Screen.PROGRESS -> ProgressScreen(
                soundClassifier = soundClassifier,
                location = location
            ) {
                onRestart()
            }
        }
    }
}

enum class Screen {
    START,
    PROGRESS
}

