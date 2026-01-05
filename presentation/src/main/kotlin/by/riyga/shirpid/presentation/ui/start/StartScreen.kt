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
import by.riyga.shirpid.presentation.R
import by.riyga.shirpid.presentation.navigation.Route
import by.riyga.shirpid.presentation.utils.AnalyticsUtil
import by.riyga.shirpid.presentation.utils.LocalNavController
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
                        AnalyticsUtil.logEvent("navigate_to_settings")
                        navController.navigate(Route.Settings)
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
        bottomBar = {
            if (state.allPermissionsGranted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp)
                ) {
                    Button(
                        onClick = {
                            AnalyticsUtil.logEvent("navigate_to_progress")
                            state.currentLocation?.let {
                                navController.navigate(Route.Progress)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_mic),
                            contentDescription = stringResource(R.string.start_record_desc),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
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
                } else if (!state.isLoadingLocation) {
                    MainActionButton(
                        onShowHistory = {
                            AnalyticsUtil.logEvent("navigate_to_history")
                            navController.navigate(Route.Archive)
                        },
                        onShowFile = {
                            AnalyticsUtil.logEvent("navigate_to_file")
                            navController.navigate(Route.File)
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
            Text(text = buttonText, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun MainActionButton(
    onShowHistory: () -> Unit,
    onShowFile: () -> Unit
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
        OutlinedButton(
            onClick = onShowHistory,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.show_history))
        }
        Spacer(Modifier.size(16.dp))
        OutlinedButton(
            onClick = onShowFile,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.open_file))
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

@Preview(showBackground = true)
@Composable
private fun PreviewSettingsRedirectDialog() {
    MaterialTheme {
        SettingsRedirectDialog(onDismiss = {}, onConfirm = {})
    }
}