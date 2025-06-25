package com.riyga.identifier.presentation.ui

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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riyga.identifier.R
import com.riyga.identifier.utils.RecognizeService
import kotlinx.coroutines.delay
import com.riyga.identifier.utils.toStringLocation
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProgressScreen(
    viewModel: IdentifierViewModel = koinViewModel(),
    onStop: (Boolean) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var birds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val highlightedBirds = remember { mutableStateMapOf<String, Boolean>() }
    val haptic = LocalHapticFeedback.current
    val timer = remember { mutableStateOf("00:00.0") }
    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {

        val startTime = System.currentTimeMillis()
        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            timer.value = String.format(
                "%02d:%02d.%d",
                (elapsed / 1000) / 60,
                (elapsed / 1000) % 60,
                (elapsed % 1000) / 100
            )
            delay(100) // Обновление каждые 100 мс
        }
    }

    LaunchedEffect(Unit) {
        RecognizeService.birdsEvents
            .collect { (bird, percent) ->
                if (percent * 100 > 30) {
                    if (bird !in birds) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }

                    birds = birds + bird

                    // Устанавливаем подсветку для элемента
                    highlightedBirds[bird] = true
                    // Убираем подсветку через 1 секунду
                    delay(1000)
                    highlightedBirds[bird] = false
                } else {
                    println("less than 30% -> $percent = $bird")
                }
            }
    }

    LaunchedEffect(birds.size) {
        if (birds.size > 1) {
            lazyListState.animateScrollToItem(birds.size - 1)
        }
    }

    Layout(
        identifiedBirds = birds,
        highlightedBirds = highlightedBirds,
        onStop = {
            onStop.invoke(it)
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
                        if (place != null) {
                            Text(
                                text = place,
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
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)
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