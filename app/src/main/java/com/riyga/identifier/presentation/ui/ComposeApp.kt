package com.riyga.identifier.presentation.ui

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
import com.riyga.identifier.utils.LocationHelper
import com.riyga.identifier.utils.SoundClassifier
import org.koin.compose.viewmodel.koinViewModel

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
    val viewModel: IdentifierViewModel = koinViewModel()

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
                viewModel.startTrackingLocation()

                // TODO delete locationHelper
                locationHelper.getCurrentLocation()?.let {
                    soundClassifier.runMetaInterpreter(it)
                }
            }
        }
    )
}

@Composable
private fun MainLayout(
    screenState: Screen,
    soundClassifier: SoundClassifier,
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

