package by.riyga.shirpid.presentation.ui.record

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import by.riyga.shirpid.data.models.Record
import by.riyga.shirpid.presentation.navigation.Route
import org.koin.compose.viewmodel.koinViewModel
import by.riyga.shirpid.presentation.models.IdentifiedBird
import by.riyga.shirpid.presentation.player.PlayerState
import by.riyga.shirpid.presentation.utils.LocalNavController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import by.riyga.shirpid.presentation.R
import by.riyga.shirpid.presentation.ui.components.BirdDatePickerDialog
import by.riyga.shirpid.presentation.ui.components.BirdScaffold
import by.riyga.shirpid.presentation.ui.components.BirdTopAppBar
import by.riyga.shirpid.presentation.ui.components.MapPointPicker
import by.riyga.shirpid.presentation.ui.components.Player
import by.riyga.shirpid.presentation.ui.components.RecordSettingsBottomSheet
import by.riyga.shirpid.presentation.utils.AnalyticsUtil
import by.riyga.shirpid.presentation.utils.deleteAudio
import by.riyga.shirpid.presentation.utils.formatSecondsRange
import by.riyga.shirpid.presentation.utils.getConfidenceColor
import by.riyga.shirpid.presentation.utils.isAudioExists
import by.riyga.shirpid.presentation.utils.share
import by.riyga.shirpid.presentation.utils.toPercentString
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    recordId: Long?,
    fileUri: String?,
    fromArchive: Boolean = false
) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val viewModel: RecordViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsState()
    val effect by viewModel.effect.collectAsStateWithLifecycle(null)
    val mediaState by viewModel.mediaState.collectAsState(initial = PlayerState())
    var showDatePicker by remember { mutableStateOf(false) }
    var showSettingsBottomSheet by remember { mutableStateOf(false) }
    var showMap by remember { mutableStateOf(false) }
    val dayPickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= Clock.System.now().toEpochMilliseconds()
            }

            override fun isSelectableYear(year: Int): Boolean {
                val yearNow =
                    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
                return year <= yearNow
            }
        }
    ).apply {
        selectedDateMillis = state.record?.timestamp
    }

    fun onBack() {
        if (fromArchive) {
            navController.popBackStack()
        } else {
            navController.popBackStack(Route.Start, false)
        }
    }

    LaunchedEffect(Unit) {
        if (recordId != null) {
            viewModel.getRecordById(recordId = recordId, context = context)
        }

        if (fileUri != null) {
            viewModel.createRecordFromAudioFile(
                context = context,
                uri = fileUri.toUri()
            )
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

    Layout(
        state = state,
        onBack = ::onBack,
        onShare = {
            AnalyticsUtil.logEvent("share_record")
            state.record?.audioFilePath?.let { audio ->
                share(
                    context = context,
                    subject = audio.toUri(),
                    chooserText = null
                )
            }
        },
        onNavigateToArchive = {
            AnalyticsUtil.logEvent("navigate_to_history")
            navController.popBackStack()
            navController.navigate(Route.Archive)
        },
        onNavigateToProgress = {
            AnalyticsUtil.logEvent("navigate_to_progress")
            navController.popBackStack()
            navController.navigate(Route.Progress)
        },
        mediaState = mediaState,
        onPlayAudio = {
            viewModel.playAudio()
        },
        onPauseAudio = {
            viewModel.pauseAudio()
        },
        fromArchive = fromArchive,
        onShowRecordSettings = {
            showSettingsBottomSheet = true
        }
    )

    if (showDatePicker) {
        BirdDatePickerDialog(
            state = dayPickerState,
            onDismiss = { showDatePicker = false },
            onDatePick = {
                showDatePicker = false
                it?.let { viewModel.setDate(it) }
            }
        )
    }

    if (showSettingsBottomSheet) {
        RecordSettingsBottomSheet(
            onChooseDefaultLocation = {
                showSettingsBottomSheet = false
                viewModel.setDefaultLocation()
            },
            onShowMap = {
                showSettingsBottomSheet = false
                showMap = true
            },
            onDismiss = {
                showSettingsBottomSheet = false
            },
            onRemoveRecord = {
                AnalyticsUtil.logEvent("delete_record")
                viewModel.setEvent(RecordContract.Event.RemoveRecord)
                showSettingsBottomSheet = false
            },
            onChangeDate = {
                showDatePicker = true
                showSettingsBottomSheet = false
            }
        )
    }

    AnimatedVisibility(
        visible = showMap,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        MapPointPicker(
            initLatitude = state.record?.latitude ?: state.currentLocation?.latitude,
            initLongitude = state.record?.longitude ?: state.currentLocation?.longitude,
            onSelected = { lat, lon ->
                showMap = false
                viewModel.setLocationFromMap(lat, lon)
            },
            onDismiss = { showMap = false }
        )
    }
}

@Composable
private fun Layout(
    state: RecordContract.State,
    fromArchive: Boolean = false,
    mediaState: PlayerState,
    onShowRecordSettings: () -> Unit,
    onPlayAudio: () -> Unit,
    onPauseAudio: () -> Unit,
    onNavigateToProgress: () -> Unit,
    onNavigateToArchive: () -> Unit,
    onShare: () -> Unit,
    onBack: () -> Unit
) {
    val record = state.record

    BirdScaffold(
        topBar = {
            BirdTopAppBar(
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = onShare
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_share),
                            contentDescription = "Share"
                        )
                    }
                    IconButton(
                        onClick = onShowRecordSettings
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_more_vert),
                            contentDescription = "show record settings"
                        )
                    }
                }
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
                    OutlinedButton(
                        onClick = onNavigateToArchive,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        )
                    ) {
                        Text(stringResource(R.string.archive))
                    }
                    Spacer(Modifier.size(8.dp))
                    Button(
                        onClick = onNavigateToProgress,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.new_recording))
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            RecordInfo(record)

            if (record != null) {
                Player(
                    mediaState = mediaState,
                    onPlay = onPlayAudio,
                    onPause = onPauseAudio,
                    modifier = Modifier.padding(top = 16.dp),
                    fileName = state.fileName
                )
            }

            if (state.classifyProgressPercent != 0) {
                Spacer(modifier = Modifier.height(32.dp))
                LinearProgressIndicator(
                    progress = { state.classifyProgressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )
                Text(
                    text = "${state.classifyProgressPercent}%",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = paddingValues.calculateBottomPadding()),
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (record?.birds.isNullOrEmpty() && record?.latitude != null) {
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

                if (record?.latitude == null) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TextButton(
                                onClick = onShowRecordSettings
                            ) {
                                Text("Необходимо добавить локацию")
                            }
                        }
                    }
                }

                state.birds.forEach { chunk ->
                    item {
                        Column {
                            Text(
                                text = formatSecondsRange(chunk.key * 3, (chunk.key + 1) * 3),
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

@Composable
private fun RecordInfo(record: Record?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
    ) {
        if (record?.locationName != null) {
            Text(
                text = record.locationName ?: "",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        if (record?.latitude != null && record.longitude != null) {
            Text(
                text = stringResource(
                    R.string.coordinates,
                    String.format("%.4f", record.latitude),
                    String.format("%.4f", record.longitude)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (record?.timestamp != null) {
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bird.comName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (bird.name.contains("_")) {
                    Text(
                        text = bird.latName,
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
                    modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun PreviewItem2() {
    DetectedBirdCard(
        bird = IdentifiedBird(
            index = 1,
            name = "Latin name_common name common name common name",
            confidence = 0.3555F
        )
    )
}