package by.riyga.shirpid.presentation.ui.start

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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import by.riyga.shirpid.R
import by.riyga.shirpid.presentation.ui.Route
import by.riyga.shirpid.utils.LocalNavController
import data.models.LocationData
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(
    viewModel: StartScreenViewModel = koinViewModel()
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        activity?.let {
            viewModel.onPermissionResult(it, isGranted)
        }
    }

    LaunchedEffect(state.isFineLocationGranted) {
        if (state.isFineLocationGranted) {
            viewModel.fetchLocationAndProceed()
        }
    }

    LaunchedEffect(viewModel.eventFlow) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Icon(
                        painter = painterResource(R.drawable.ic_logo),
                        contentDescription = stringResource(R.string.settings),
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(by.riyga.shirpid.presentation.ui.Route.Settings)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { paddings ->
        Box(
            modifier = Modifier
                .padding(paddings)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                if (state.isLoadingLocation) {
                    CircularProgressIndicator()
                } else if (state.locationError != null) {
                    Text(
                        stringResource(R.string.location_error, state.locationError ?: "-"),
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = { viewModel.fetchLocationAndProceed() }) {
                        Text(stringResource(R.string.try_again))
                    }
                } else {
                    state.currentLocation?.let {
                        Text("${it.latitude}, ${it.longitude}", fontSize = 12.sp)
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center)
            ) {
                if (!state.allPermissionsGranted) {
                    PermissionRequestSection(
                        uiState = state,
                        onAudioPermissionClick = {
                            viewModel.onPermissionRequested(Manifest.permission.RECORD_AUDIO)
                        },
                        onLocationPermissionClick = {
                            viewModel.onPermissionRequested(Manifest.permission.ACCESS_FINE_LOCATION)
                        },
                        onNotificationPermissionClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                viewModel.onPermissionRequested(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                } else if (!state.isLoadingLocation && state.locationError == null) {
                    MainActionButton(
                        onStart = {
                            state.currentLocation?.let {
                                navController.navigate(Route.Progress)
                            }
                        },
                        onShowHistory = {
                            navController.navigate(Route.BirdHistory)
                        }
                    )
                }
            }
        }
    }

    if (state.showSettingsDialog) {
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!uiState.isAudioGranted) {
            PermissionItem(
                buttonText = stringResource(R.string.permission_audio_request),
                description = stringResource(R.string.permission_audio_description),
                onClick = onAudioPermissionClick
            )
        }
        if (!uiState.isFineLocationGranted) {
            PermissionItem(
                buttonText = stringResource(R.string.permission_location_request),
                description = stringResource(R.string.permission_location_description),
                onClick = onLocationPermissionClick
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !uiState.isNotificationsGranted) {
            PermissionItem(
                buttonText = stringResource(R.string.permission_notification_request),
                description = stringResource(R.string.permission_notification_description),
                onClick = onNotificationPermissionClick
            )
        }
    }
}

@Composable
private fun PermissionItem(
    buttonText: String,
    description: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface,
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        OutlinedButton(onClick = onClick) {
            Text(text = buttonText)
        }
    }
}

@Composable
fun MainActionButton(
    onStart: () -> Unit,
    onShowHistory: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.size(16.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Icon(
                painter = painterResource(R.drawable.ic_mic),
                contentDescription = stringResource(R.string.start_record_desc),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.size(16.dp))
        OutlinedButton(
            onClick = onShowHistory,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.show_history))
        }
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