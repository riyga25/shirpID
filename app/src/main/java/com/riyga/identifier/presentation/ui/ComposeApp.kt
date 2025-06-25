package com.riyga.identifier.presentation.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import android.os.IBinder
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.riyga.identifier.utils.LocationHelper
import com.riyga.identifier.utils.RecognizeService
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ComposeApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var screenState by remember { mutableStateOf(Screen.START) }
    val locationHelper = remember { LocationHelper(context) }
    val viewModel: IdentifierViewModel = koinViewModel()
    var location: Location? = remember { null }

    var bound by remember { mutableStateOf(false) }
    var service: RecognizeService? by remember { mutableStateOf(null) }
    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                val localBinder = binder as RecognizeService.LocalBinder
                service = localBinder.getService()
                bound = true
                location?.let {l ->
                    service?.startForegroundService(l.latitude.toFloat(), l.longitude.toFloat())
                }
            }

            override fun onServiceDisconnected(name: ComponentName) {
                bound = false
                service = null
            }
        }
    }

//    LaunchedEffect(screenState) {
//        if (screenState == Screen.PROGRESS) {
//            viewModel.startTrackingLocation()
//
//            // TODO delete locationHelper
//            locationHelper.getCurrentLocation()?.let {
//                soundClassifier.runMetaInterpreter(it)
//            }
//        }
//    }

    MainLayout(
        screenState = screenState,
        onStart = {
            scope.launch {
                locationHelper.getCurrentLocation()?.let {
                    if (service != null) {
                        service?.startForegroundService(it.latitude.toFloat(), it.longitude.toFloat())
                    } else {
                        location = it
                        Intent(context, RecognizeService::class.java).also { intent ->
                            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
                        }
                    }
                    screenState = Screen.PROGRESS
                }
            }
        },
        onStop = {
            service?.stop(it)
            screenState = Screen.START
        }
    )
}

@Composable
private fun MainLayout(
    screenState: Screen,
    onStart: () -> Unit,
    onStop: (Boolean) -> Unit
) {
    MaterialTheme {
        when (screenState) {
            Screen.START -> StartScreen(onStart = onStart)
            Screen.PROGRESS -> ProgressScreen(onStop = onStop)
        }
    }
}

enum class Screen {
    START,
    PROGRESS
}

