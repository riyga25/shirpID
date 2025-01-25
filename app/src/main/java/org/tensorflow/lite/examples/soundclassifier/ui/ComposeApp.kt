package org.tensorflow.lite.examples.soundclassifier.ui

import androidx.compose.material.MaterialTheme
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
    val locationHelper = remember { LocationHelper(context) }

    MainLayout(
        screenState = screenState,
        onStart = {
            scope.launch {
                locationHelper.getCurrentLocation()?.let {
                    soundClassifier.runMetaInterpreter(it)
                }
            }

            screenState = Screen.PROGRESS
        },
        onRestart = {
            screenState = Screen.START
        },
        soundClassifier = soundClassifier
    )
}

@Composable
private fun MainLayout(
    screenState: Screen,
    soundClassifier: SoundClassifier,
    onStart: () -> Unit,
    onRestart: () -> Unit
) {
    MaterialTheme {
        when (screenState) {
            Screen.START -> StartScreen {
                onStart()
            }

            Screen.PROGRESS -> ProgressScreen(
                soundClassifier = soundClassifier
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

