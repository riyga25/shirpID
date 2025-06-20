package com.riyga.identifier.presentation.ui

import android.Manifest
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.riyga.identifier.utils.LocationHelper
import com.riyga.identifier.utils.SoundClassifier
import com.riyga.identifier.utils.isPermissionGranted
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

    LaunchedEffect(Unit) {
        val audio = isPermissionGranted(
            context,
            Manifest.permission.RECORD_AUDIO
        )
        val location = isPermissionGranted(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (audio && location) {
            screenState = Screen.PROGRESS
        }
    }

    LaunchedEffect(screenState) {
        if (screenState == Screen.PROGRESS) {
            viewModel.startTrackingLocation()

            // TODO delete locationHelper
            locationHelper.getCurrentLocation()?.let {
                soundClassifier.runMetaInterpreter(it)
            }
        }
    }

    MainLayout(
        screenState = screenState,
        onStart = {
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
            Screen.START -> StartScreen{
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

