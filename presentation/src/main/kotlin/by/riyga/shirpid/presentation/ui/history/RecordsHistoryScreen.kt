package by.riyga.shirpid.presentation.ui.history

import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import by.riyga.shirpid.data.models.Record
import by.riyga.shirpid.presentation.navigation.Route
import by.riyga.shirpid.presentation.utils.LocalNavController
import org.koin.compose.viewmodel.koinViewModel
import java.text.SimpleDateFormat
import java.util.*
import by.riyga.shirpid.presentation.R
import by.riyga.shirpid.presentation.ui.components.BirdScaffold
import by.riyga.shirpid.presentation.ui.components.BirdTopAppBar
import by.riyga.shirpid.presentation.utils.AnalyticsUtil
import by.riyga.shirpid.presentation.utils.deleteAudio
import by.riyga.shirpid.presentation.utils.isAudioExists
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirdHistoryScreen(
    viewModel: BirdHistoryViewModel = koinViewModel()
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            navController.navigate(Route.Record(fileUri = uri.toString()))
        }
    }

    LaunchedEffect(state.records) {
        if (state.records.isNotEmpty()) {
            val notFoundAudioIds = state.records.mapNotNull { record ->
                val isExist = context.isAudioExists(record.audioFilePath)
                if (isExist) null else record.id
            }
            if (notFoundAudioIds.isNotEmpty()) {
                println("REMOVE RECORDS: ${notFoundAudioIds.joinToString()}")
                viewModel.removeRecord(notFoundAudioIds)
            }
        }
    }

    BirdScaffold(
        topBar = {
            BirdTopAppBar(
                title = stringResource(id = R.string.archive),
                onBack = { navController.navigateUp() },
                actions = {
                    if (state.totalRecords > 0) {
                        IconButton(
                            onClick = { showDeleteAllDialog = true }
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.delete_all)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                OutlinedButton(
                    onClick = {
                        AnalyticsUtil.logEvent("navigate_to_file")
                        filePickerLauncher.launch("audio/x-wav")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(stringResource(R.string.open_file))
                }
                Spacer(Modifier.size(8.dp))
                Button(
                    onClick = {
                        navController.popBackStack()
                        navController.navigate(Route.Progress)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.new_recording))
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Statistics Card
            StatisticsCard(
                totalRecords = state.totalRecords,
                uniqueSpecies = state.uniqueSpecies,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Loading state
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.records.isEmpty()) {
                // Empty state
                EmptyStateCard(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            } else {
                // Records list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.records, key = { it.timestamp }) { record ->
                        RecordCard(
                            record = record,
                            onClick = {
                                AnalyticsUtil.logEvent("navigate_to_record")
                                navController.navigate(
                                    Route.Record(record.id, true)
                                )
                            }
                        )
                    }
                }

            }
        }
    }

    // Delete all confirmation dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(stringResource(R.string.delete_all_records)) },
            text = { Text(stringResource(R.string.delete_all_records_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.records.map { it.audioFilePath }.forEach {
                            context.deleteAudio(it.toUri())
                        }
                        AnalyticsUtil.logEvent("delete_all_records")
                        viewModel.deleteAllRecords()
                        showDeleteAllDialog = false
                    }
                ) {
                    Text(stringResource(R.string.delete_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun StatisticsCard(
    modifier: Modifier = Modifier,
    totalRecords: Int,
    uniqueSpecies: Int
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(16.dp)
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatisticItem(
            label = stringResource(R.string.total_records),
            value = totalRecords.toString()
        )
        StatisticItem(
            label = stringResource(R.string.unique_species),
            value = uniqueSpecies.toString()
        )
    }
}

@Composable
fun StatisticItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyStateCard(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🐦",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_records_saved_yet),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun RecordCard(
    modifier: Modifier = Modifier,
    record: Record,
    onClick: () -> Unit = {}
) {
    val birdsSize = record.birds.size
    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    val dateStr = dateFormat.format(Date(record.timestamp))
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = timeFormat.format(Date(record.timestamp))
    val locationStr = record.locationName
        ?: if (record.latitude != null && record.longitude != null) {
            "${String.format("%.4f", record.latitude)}, ${
                String.format(
                    "%.4f",
                    record.longitude
                )
            }"
        } else {
            stringResource(R.string.unknown_location)
        }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "$dateStr - $locationStr",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$timeStr • ${
                pluralStringResource(
                    R.plurals.birds_count,
                    birdsSize,
                    birdsSize
                )
            }",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview
@Composable
private fun Preview() {
    BirdHistoryScreen()
}