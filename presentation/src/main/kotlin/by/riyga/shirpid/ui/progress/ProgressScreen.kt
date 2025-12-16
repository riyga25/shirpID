package by.riyga.shirpid.ui.progress

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.riyga.shirpid.presentation.R
import by.riyga.shirpid.models.IdentifiedBird
import by.riyga.shirpid.ui.Route
import by.riyga.shirpid.utils.LocalNavController
import by.riyga.shirpid.utils.RecognizeService
import by.riyga.shirpid.utils.toStringLocation
import kotlinx.coroutines.flow.Flow
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val effect by viewModel.effect.collectAsStateWithLifecycle(null)
    val haptic = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val serviceTitle = stringResource(R.string.app_name)
    val serviceDescription = stringResource(R.string.eavesdrop)

    var service: RecognizeService? by remember { mutableStateOf(null) }
    var isBound by remember { mutableStateOf(false) }
    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                val localBinder = binder as RecognizeService.LocalBinder
                service = localBinder.getService()
                isBound = true

                if (state.location != null) {
                    service?.startForegroundService(
                        latitude = state.location?.latitude?.toFloat() ?: 0F,
                        longitude = state.location?.longitude?.toFloat() ?: 0F,
                        title = serviceTitle,
                        description = serviceDescription
                    )
                }
            }

            override fun onServiceDisconnected(name: ComponentName) {
                service = null
                isBound = false
            }
        }
    }

    // Привязка/отвязка сервиса при входе/выходе с экрана
    DisposableEffect(Unit) {
        Intent(context, RecognizeService::class.java).also { intent ->
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
        onDispose {
            if (isBound) {
                context.unbindService(connection)
                isBound = false // Убедимся, что состояние обновлено
            }
        }
    }

    LaunchedEffect(effect) {
        when (val castEffect = effect) {
            is ProgressContract.Effect.NotifyByHaptic -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }

            is ProgressContract.Effect.ShowResult -> {
                navController.popBackStack()
                navController.navigate(Route.DetectionResult(castEffect.id, false))
            }

            else -> {}
        }
    }

    LaunchedEffect(isBound) {
        if (isBound) {
            viewModel.setEvent(ProgressContract.Event.StartTimer())
            RecognizeService.birdsEvents.collect { (bird, percent) ->
                viewModel.setEvent(
                    ProgressContract.Event.AddIdentification(bird, percent)
                )
            }
        } else {
            viewModel.setEvent(ProgressContract.Event.StopTimer())
        }
    }

    LaunchedEffect(state.birds.size) {
        if (state.birds.size > 1) {
            lazyListState.animateScrollToItem(state.birds.size - 1)
        }
    }

    Layout(
        onStop = { saveRecord ->
            val audio = service?.stop(saveRecord)
            if (saveRecord) {
                if (audio != null) {
                    viewModel.setEvent(ProgressContract.Event.SaveRecord(audio))
                } else {
                    // TODO show error ?!
                }
            } else {
                navController.navigateUp()
            }
        },
        timerFlow = viewModel.timer,
        listState = lazyListState,
        state = state
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Layout(
    state: ProgressContract.State,
    listState: LazyListState,
    timerFlow: Flow<Long>,
    onStop: (saveRecord: Boolean) -> Unit
) {
    val place = state.locationInfo.toStringLocation()
    val location = state.location.toStringLocation()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        if (!place.isNullOrEmpty()) {
                            Text(
                                text = place,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        if (state.loading) {
                            CircularProgressIndicator()
                        } else {
                            location?.let { loc ->
                                if (loc.isNotEmpty()) {
                                    Text(
                                        text = loc,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            RecordingControls(
                onCancel = { onStop(false) },
                onSave = { onStop(true) },
                timerFlow = timerFlow
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { paddings ->
        LazyColumn(
            contentPadding = PaddingValues(
                bottom = paddings.calculateBottomPadding(),
                top = paddings.calculateTopPadding()
            ),
            state = listState
        ) {
            state.birds.forEach {
                item {
                    BirdRow(
                        bird = it.value.name,
                        isHighlighted = it.key in state.currentlyHeardBirds
                    )
                }
            }
        }
    }
}

@Composable
fun RecordingControls(
    onCancel: () -> Unit,
    onSave: () -> Unit,
    timerFlow: Flow<Long>
) {
    val timerValue by timerFlow.collectAsStateWithLifecycle(0L)

    Box(
        modifier = Modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(8.dp)
    ) {
        TextButton(
            onClick = onCancel,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Text(
                text = stringResource(R.string.cancel_recording),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Таймер
        Text(
            text = formatTime(timerValue),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(end = 12.dp)
        )

        // Кнопка "Сохранить"
        TextButton(
            onClick = onSave,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Text(
                text = stringResource(R.string.stop_recording),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun BirdRow(bird: String, isHighlighted: Boolean) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isHighlighted) Color.Yellow.copy(alpha = 0.3F) else Color.Transparent,
        animationSpec = tween(durationMillis = 500)
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = bird.substringAfter("_"),
                fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Normal
            )
        }
        HorizontalDivider()
    }
}

private fun formatTime(elapsedMs: Long): String {
    val totalSeconds = elapsedMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}