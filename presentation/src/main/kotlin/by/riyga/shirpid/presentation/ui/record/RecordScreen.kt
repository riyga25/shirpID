package by.riyga.shirpid.presentation.ui.record

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.riyga.shirpid.presentation.ui.Route
import org.koin.compose.viewmodel.koinViewModel
import by.riyga.shirpid.presentation.models.IdentifiedBird
import by.riyga.shirpid.presentation.player.PlayerState
import by.riyga.shirpid.presentation.utils.LocalNavController
import org.koin.core.parameter.parametersOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import by.riyga.shirpid.presentation.R
import by.riyga.shirpid.presentation.utils.AnalyticsUtil
import by.riyga.shirpid.presentation.utils.deleteAudio
import by.riyga.shirpid.presentation.utils.getConfidenceColor
import by.riyga.shirpid.presentation.utils.isAudioExists
import by.riyga.shirpid.presentation.utils.toPercentString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    recordId: Long,
    fromArchive: Boolean = false
) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val viewModel: RecordViewModel = koinViewModel {
        parametersOf(recordId)
    }
    val state by viewModel.uiState.collectAsState()
    val effect by viewModel.effect.collectAsStateWithLifecycle(null)
    val mediaState by viewModel.mediaState.collectAsState(initial = PlayerState())

    val record = state.record

    fun onBack() {
        if (fromArchive) {
            navController.popBackStack()
        } else {
            navController.popBackStack(Route.Start, false)
        }
    }

    LaunchedEffect(effect) {
        when (val castEffect = effect) {
            is RecordContract.Effect.RecordRemoved -> {
                context.deleteAudio(castEffect.uri.toUri())
                onBack()
            }

            else -> {}
        }
    }

    // audio exist double check after history list
    LaunchedEffect(state.record) {
        state.record?.let {
            val isExist = context.isAudioExists(it.audioFilePath)
            if (!isExist) {
                viewModel.setEvent(RecordContract.Event.RemoveRecord)
            }
        }
    }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = { onBack() }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(by.riyga.shirpid.presentation.R.string.back_to_start)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            AnalyticsUtil.logEvent("share_record")
                            record?.audioFilePath?.let { audio ->
                                share(
                                    context = context,
                                    subject = audio.toUri(),
                                    chooserText = null
                                )
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share"
                        )
                    }
                    IconButton(
                        onClick = {
                            AnalyticsUtil.logEvent("delete_record")
                            viewModel.setEvent(RecordContract.Event.RemoveRecord)
                        }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_all)
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
            if (!fromArchive) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick = {
                            AnalyticsUtil.logEvent("navigate_to_history")
                            navController.popBackStack()
                            navController.navigate(Route.Archive)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(stringResource(R.string.archive))
                    }
                    Spacer(Modifier.size(8.dp))
                    Button(
                        onClick = {
                            AnalyticsUtil.logEvent("navigate_to_progress")
                            navController.popBackStack()
                            navController.navigate(Route.Progress)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.new_recording))
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            if (record?.latitude != null && record.longitude != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp)
                ) {
                    if (record.locationName != null) {
                        Text(
                            text = record.locationName ?: "",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = stringResource(
                            R.string.coordinates,
                            String.format(
                                "%.4f",
                                record.latitude
                            ),
                            String.format("%.4f", record.longitude)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.recorded,
                            SimpleDateFormat(
                                "dd MMMM yyyy HH:mm",
                                Locale.getDefault()
                            ).format(Date(record.timestamp))
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (record != null) {
                Box(
                    Modifier
                        .padding(top = 16.dp)
                        .background(
                            colorScheme.tertiary.copy(alpha = 0.3f), RoundedCornerShape(8.dp)
                        )
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    if (mediaState.progress > 0) {
                        val progressValue = if (mediaState.duration != 0L) {
                            mediaState.progress.toFloat() / mediaState.duration
                        } else 0f

                        LinearProgressIndicator(
                            progress = { progressValue },
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart),
                            gapSize = 0.dp,
                            drawStopIndicator = {}
                        )
                    }
                    Text(
                        text = "${formatTime(mediaState.progress)}/${formatTime(mediaState.duration)}",
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopEnd),
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (mediaState.isPlaying) {
                                Icons.Default.PauseCircle
                            } else {
                                Icons.Default.PlayCircle
                            },
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(
                                    role = Role.Button,
                                    onClick = {
                                        AnalyticsUtil.logEvent("click_play_stop_button")
                                        if (mediaState.isPlaying) {
                                            viewModel.pauseAudio()
                                        } else {
                                            viewModel.playAudio()
                                        }
                                    }
                                ),
                            tint = colorScheme.tertiary
                        )
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = paddingValues.calculateBottomPadding()),
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (record?.birds.isNullOrEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.no_birds_in_recording),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                items(state.birds) { bird ->
                    DetectedBirdCard(bird)
                }
            }
        }
    }
}

@Composable
fun DetectedBirdCard(
    bird: IdentifiedBird,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bird.name.substringAfter("_").takeIf { it != bird.name } ?: bird.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (bird.name.contains("_")) {
                    Text(
                        text = bird.comName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Confidence badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = bird.confidence.getConfidenceColor()
            ) {
                Text(
                    text = bird.confidence.toPercentString(),
                    modifier = Modifier.padding(4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun share(
    context: Context,
    subject: Uri,
    chooserText: String?
) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, subject)
        type = "audio/x-wav"
    }
    val shareIntent = Intent.createChooser(sendIntent, chooserText)
    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    ContextCompat.startActivity(context, shareIntent, null)
}

private fun formatTime(millis: Long?): String {
    if (millis == null) return ""

    val timeMin = millis / 60000
    val timeSec = (millis / 1000) % 60

    val minString = if (timeMin < 10) "0$timeMin" else "$timeMin"
    val secString = if (timeSec < 10) "0$timeSec" else "$timeSec"

    return "$minString:$secString"
}