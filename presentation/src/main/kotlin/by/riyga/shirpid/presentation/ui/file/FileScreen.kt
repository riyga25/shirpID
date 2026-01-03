package by.riyga.shirpid.presentation.ui.file

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.riyga.shirpid.presentation.R
import by.riyga.shirpid.presentation.player.PlayerState
import by.riyga.shirpid.presentation.ui.components.Player
import by.riyga.shirpid.presentation.ui.record.DetectedBirdCard
import by.riyga.shirpid.presentation.utils.LocalNavController
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FileScreen() {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val viewModel: FileViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val mediaState by viewModel.mediaState.collectAsState(initial = PlayerState())

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        scope.launch {
            viewModel.onFileSelected(context, uri)
        }
    }

    FileLayout(
        fileName = state.fileName,
        onPickFile = { filePickerLauncher.launch("audio/x-wav") },
        onBack = { navController.popBackStack() },
        state = state,
        mediaState = mediaState,
        onPlay = {
            viewModel.playAudioPlayer()
        },
        onPause = {
            viewModel.pauseAudioPlayer()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileLayout(
    state: FileContract.State,
    fileName: String,
    mediaState: PlayerState,
    onPickFile: () -> Unit = {},
    onBack: () -> Unit = {},
    onPause: () -> Unit = {},
    onPlay: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.open_file))
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_to_start)
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
            if (state.fileName.isEmpty()) {
                Box(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                        .navigationBarsPadding()
                ) {
                    Button(onPickFile, Modifier.fillMaxWidth()) {
                        Text(text = stringResource(R.string.choose_file))
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { paddings ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = paddings.calculateTopPadding()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (fileName.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text(
                        text = stringResource(R.string.file_name, fileName),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp)
                    )
                    Player(
                        mediaState = mediaState,
                        onPlay = onPlay,
                        onPause = onPause
                    )
                }
                if (state.progressPercent < 100) {
                    Spacer(modifier = Modifier.height(32.dp))
                    LinearProgressIndicator(
                        progress = { state.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                    Text(
                        text = "${state.progressPercent}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = paddings.calculateBottomPadding()),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                if (state.progressPercent == 100 && state.birds.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_birds_in_recording),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                state.birds.forEach { chunk ->
                    item {
                        Column {
                            Text(
                                text = formatRange(chunk.key * 3, (chunk.key + 1) * 3),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            chunk.value.forEachIndexed { i, bird ->
                                if (i != 0) {
                                    Spacer(Modifier.size(4.dp))
                                }
                                DetectedBirdCard(bird)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatSeconds(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}

fun formatRange(startSeconds: Int, endSeconds: Int): String {
    val startTime = formatSeconds(startSeconds)
    val endTime = formatSeconds(endSeconds)
    return "$startTime - $endTime"
}
