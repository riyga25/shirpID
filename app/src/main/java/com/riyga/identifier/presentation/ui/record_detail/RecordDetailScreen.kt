package com.riyga.identifier.presentation.ui.record_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.riyga.identifier.data.models.Record
import com.riyga.identifier.presentation.ui.history.BirdHistoryViewModel
import org.koin.compose.viewmodel.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailScreen(
    navController: NavController,
    recordId: Long,
    viewModel: BirdHistoryViewModel = koinViewModel()
) {
    var record by remember { mutableStateOf<Record?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch record by timestamp
    LaunchedEffect(recordId) {
        try {
            record = viewModel.getRecordByTimestamp(recordId)
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Record Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            record?.let {
                                viewModel.deleteRecord(it)
                                navController.navigateUp()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete All")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (record == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Record not found")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                if (record?.latitude != null && record?.longitude != null) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(16.dp)
                        ) {
                            if (record!!.locationName != null) {
                                Text(
                                    text = record!!.locationName!!,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Text(
                                text = "Coordinates: ${
                                    String.format(
                                        "%.4f",
                                        record!!.latitude
                                    )
                                }, ${String.format("%.4f", record!!.longitude)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Recorded: ${
                                    SimpleDateFormat(
                                        "MMM dd, yyyy HH:mm",
                                        Locale.getDefault()
                                    ).format(Date(record!!.timestamp))
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (record?.birds.isNullOrEmpty()) {
                    item {
                        Text(
                            text = "No birds detected in this recording",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }

                items(record?.birds ?: emptyList()) { bird ->
                    val birdName = bird.name
                    val parts = birdName.split("_")
                    val scientificName = parts.getOrNull(0) ?: birdName
                    val commonName = parts.getOrNull(1) ?: birdName

                    BirdItem(
                        scientificName = scientificName,
                        commonName = commonName
                    )
                }
            }
        }
    }
}

@Composable
fun BirdItem(
    scientificName: String,
    commonName: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Text(
            text = commonName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = scientificName,
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
