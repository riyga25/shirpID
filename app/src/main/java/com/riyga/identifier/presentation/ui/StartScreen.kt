package com.riyga.identifier.presentation.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.riyga.identifier.R
import org.koin.compose.viewmodel.koinViewModel

// Утилитарная функция для открытия настроек приложения
fun openAppSettings(context: Context) {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).also {
        context.startActivity(it)
    }
}

@Composable
fun StartScreen(
    onStart: () -> Unit = {},
    viewModel: StartScreenViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val uiState by viewModel.uiState.collectAsState()
    val currentOnStart by rememberUpdatedState(onStart)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        activity?.let {
            viewModel.onPermissionResult(it, isGranted)
        }
    }

    LaunchedEffect(uiState.isFineLocationGranted) {
        if (uiState.isFineLocationGranted) {
            viewModel.fetchLocationAndProceed()
        }
    }

    // Обработка событий от ViewModel
    LaunchedEffect(key1 = viewModel.eventFlow) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is StartScreenEvent.RequestPermission -> {
                    permissionLauncher.launch(event.permission)
                }

                is StartScreenEvent.OpenAppSettings -> {
                    openAppSettings(context)
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionsState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold { paddings ->
        Box(modifier = Modifier
            .padding(paddings)
            .fillMaxSize()
        ) {
            Column(
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.isLoadingLocation) {
                    CircularProgressIndicator()
                } else if (uiState.locationError != null) {
                    Text("Error: ${uiState.locationError}", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.fetchLocationAndProceed() }) {
                        Text("Try Again")
                    }
                } else {
                    uiState.currentLocation?.let {
                        Text("Coordinates: ${it.latitude}, ${it.longitude}")
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center)
            ) {
                if (!uiState.allPermissionsGranted) {
                    PermissionRequestSection(
                        uiState = uiState,
                        onAudioPermissionClick = { viewModel.onPermissionRequested(Manifest.permission.RECORD_AUDIO) },
                        onLocationPermissionClick = { viewModel.onPermissionRequested(Manifest.permission.ACCESS_FINE_LOCATION) },
                        onNotificationPermissionClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                viewModel.onPermissionRequested(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                } else if (!uiState.isLoadingLocation && uiState.locationError == null) {
                    MainActionButton(onStart = onStart)
                }
            }
        }
    }

    if (uiState.showSettingsDialog) {
        SettingsRedirectDialog(
            onDismiss = { viewModel.dismissSettingsDialog() },
            onConfirm = { viewModel.requestOpenAppSettings() }
        )
    }
}

@Composable
fun PermissionRequestSection(
    uiState: PermissionState,
    onAudioPermissionClick: () -> Unit,
    onLocationPermissionClick: () -> Unit,
    onNotificationPermissionClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!uiState.isAudioGranted) {
            OutlinedButton(onClick = onAudioPermissionClick) {
                Text(text = stringResource(R.string.permission_audio_request))
            }
        }
        if (!uiState.isFineLocationGranted) {
            OutlinedButton(onClick = onLocationPermissionClick) {
                Text(text = stringResource(R.string.permission_location_request))
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !uiState.isNotificationsGranted) {
            OutlinedButton(onClick = onNotificationPermissionClick) {
                Text(text = stringResource(R.string.permission_notification_request))
            }
        }
    }
}

@Composable
fun MainActionButton(onStart: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.rippleLoadingAnimationModifier(
                isActive = true, // Это состояние может также управляться извне, если нужно
                color = MaterialTheme.colorScheme.primary,
                expandFactor = 5f,
            )
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable(
                        role = Role.Button,
                        onClick = onStart
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_mic),
                    contentDescription = stringResource(R.string.start_record_desc),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
        Text(
            text = stringResource(R.string.sound_id_title),
            fontSize = 24.sp,
            modifier = Modifier.padding(top = 16.dp),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun SettingsRedirectDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(all = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.permission_settings_dialog_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(stringResource(R.string.permission_settings_dialog_message))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onConfirm) {
                        Text(stringResource(R.string.go_to_settings))
                    }
                }
            }
        }
    }
}

fun Modifier.rippleLoadingAnimationModifier(
    isActive: Boolean,
    color: Color,
    circles: Int = 3,
    expandFactor: Float = 5f,
    durationMillis: Int = 5000,
): Modifier {
    if (!isActive) return this

    return composed {
        val transition = rememberInfiniteTransition(label = "ripple_transition")

        val translateAnimations = List(circles) { index ->
            transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = durationMillis,
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(index * (durationMillis / circles))
                ),
                label = "ripple_circle_$index"
            )
        }

        this.drawBehind {
            val maxRadius = (maxOf(size.height, size.width) / 2) * expandFactor
            translateAnimations.forEach { animatable ->
                val alpha = 1f - animatable.value
                drawCircle(
                    color = color.copy(
                        alpha = alpha.coerceIn(
                            0f,
                            1f
                        )
                    ), // Убедимся, что alpha в границах
                    radius = maxRadius * animatable.value,
                    center = size.center
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSettingsRedirectDialog() {
    MaterialTheme {
        SettingsRedirectDialog(onDismiss = {}, onConfirm = {})
    }
}