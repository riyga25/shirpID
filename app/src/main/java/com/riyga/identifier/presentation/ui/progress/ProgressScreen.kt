package com.riyga.identifier.presentation.ui.progress

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riyga.identifier.R
import com.riyga.identifier.data.models.LocationData
import com.riyga.identifier.presentation.models.LocationInfo
import com.riyga.identifier.presentation.ui.detection_result.DetectedBird
import com.riyga.identifier.utils.LocalNavController
import com.riyga.identifier.utils.RecognizeService
import kotlinx.coroutines.delay
import com.riyga.identifier.utils.toStringLocation
import kotlinx.coroutines.isActive
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProgressScreen(
    location: LocationData,
    onNavigateToResults: (List<DetectedBird>, LocationData?, LocationInfo?, String?) -> Unit,
    viewModel: ProgressViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var identifiedBirds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val birdConfidences = remember { mutableStateMapOf<String, Float>() }
    val highlightedBirds = remember { mutableStateMapOf<String, Boolean>() }
    val haptic = LocalHapticFeedback.current
    val timer = remember { mutableStateOf("00:00.0") }
    val lazyListState = rememberLazyListState()

    var service: RecognizeService? by remember { mutableStateOf(null) }
    var isBound by remember { mutableStateOf(false) }
    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                val localBinder = binder as RecognizeService.LocalBinder
                service = localBinder.getService()
                isBound = true
                // Как только сервис привязан, запускаем его с координатами
                service?.startForegroundService(
                    location.latitude.toFloat(),
                    location.longitude.toFloat()
                )
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

    // Запуск таймера
    LaunchedEffect(isBound) { // Перезапускаем таймер, если сервис привязан (или по др. условию)
        if (isBound) { // Начинаем таймер, только когда сервис активен
            val startTime = System.currentTimeMillis()
            while (this.isActive) { // isActive из корутины
                val elapsed = System.currentTimeMillis() - startTime
                timer.value = String.format(
                    "%02d:%02d.%d",
                    (elapsed / 1000) / 60,
                    (elapsed / 1000) % 60,
                    (elapsed % 1000) / 100
                )
                delay(100)
            }
        } else {
            timer.value = "00:00.0" // Сброс таймера, если сервис не активен
        }
    }

    // Сбор событий от сервиса
    LaunchedEffect(isBound) { // Перезапускаем сборщик, если меняется состояние isBound
        if (isBound) {
            RecognizeService.birdsEvents.collect { (bird, percent) ->
                if (percent * 100 > 30) {
                    if (bird !in identifiedBirds) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        identifiedBirds = identifiedBirds + bird
                    }
                    // Track the highest confidence for each bird
                    birdConfidences[bird] = maxOf(birdConfidences[bird] ?: 0f, percent)
                    highlightedBirds[bird] = true
                    delay(1000) // Эта задержка здесь ок для UI эффекта
                    highlightedBirds[bird] = false
                } else {
                    println("ProgressScreen: less than 30% -> $percent = $bird")
                }
            }
        }
    }

    LaunchedEffect(identifiedBirds.size) {
        if (identifiedBirds.size > 1) {
            lazyListState.animateScrollToItem(identifiedBirds.size - 1)
        }
    }

    Layout(
        identifiedBirds = identifiedBirds,
        highlightedBirds = highlightedBirds,
        onStop = { saveRecord ->
            service?.stop(saveRecord)
            if (saveRecord) {
                // Convert identified birds to DetectedBird objects with their confidence
                val detectedBirdsList = identifiedBirds.map { birdName ->
                    DetectedBird(
                        name = birdName,
                        confidence = birdConfidences[birdName] ?: 0f
                    )
                }
                onNavigateToResults(detectedBirdsList, state.location, state.locationInfo, RecognizeService.audioFilePath)
            } else {
                navController.navigateUp()
            }
        },
        location = state.location.toStringLocation(),
        place = state.locationInfo.toStringLocation(),
        timer = timer,
        listState = lazyListState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Layout(
    listState: LazyListState,
    identifiedBirds: Set<String>,
    highlightedBirds: Map<String, Boolean>,
    location: String? = null,
    place: String? = null,
    timer: MutableState<String>,
    onStop: (saveRecord: Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        if (place.isNullOrEmpty().not()) {
                            Text(
                                text = place ?: "",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        if (location != null) {
                            Text(
                                text = location,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            RecordingControls(
                onCancel = { onStop(false) },
                onStop = { onStop(true) },
                timerValue = timer.value
            )
        }
    ) { paddings ->
        LazyColumn(
            contentPadding = PaddingValues(
                bottom = paddings.calculateBottomPadding(),
                top = paddings.calculateTopPadding()
            ),
            state = listState
        ) {
            items(identifiedBirds.toList()) { bird ->
                BirdRow(
                    bird = bird,
                    isHighlighted = highlightedBirds[bird] == true
                )
            }
        }
    }
}

@Composable
fun RecordingControls(
    onCancel: () -> Unit,
    onStop: () -> Unit,
    timerValue: String
) {
    Box(
        modifier = Modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 8.dp)
    ) {
        TextButton(
            onClick = onCancel,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Text(
                text = "Отменить",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Кнопка "Стоп"
        IconButton(
            onClick = onStop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error)
                .align(Alignment.Center)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_stop),
                contentDescription = "Stop Recording",
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier.size(30.dp)
            )
        }

        // Таймер
        Text(
            text = timerValue,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
        )
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