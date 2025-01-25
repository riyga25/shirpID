package org.tensorflow.lite.examples.soundclassifier.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.tensorflow.lite.examples.soundclassifier.R
import org.tensorflow.lite.examples.soundclassifier.SoundClassifier

@Composable
fun ProgressScreen(
    soundClassifier: SoundClassifier,
    onStop: () -> Unit
) {
    val isRecording by soundClassifier.isRecording.collectAsStateWithLifecycle()
    var birds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val highlightedBirds = remember { mutableStateMapOf<String, Boolean>() }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        soundClassifier.start()
    }

    LaunchedEffect(Unit) {
        soundClassifier.birdEvents
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

    Layout(
        identifiedBirds = birds,
        highlightedBirds = highlightedBirds,
        onStop = {
            soundClassifier.stop(false)
            onStop.invoke()
        }
    )
}

@Composable
private fun Layout(
    identifiedBirds: Set<String>,
    highlightedBirds: Map<String, Boolean>,
    onStop: () -> Unit
) {

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onStop,
                backgroundColor = Color.DarkGray,
                contentColor = Color.White
            ) {
                Icon(painterResource(R.drawable.ic_pause_24dp), "stop")
            }
        }
    ) { paddings ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = paddings.calculateBottomPadding()),
            modifier = Modifier.statusBarsPadding()
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
        Divider()
    }
}