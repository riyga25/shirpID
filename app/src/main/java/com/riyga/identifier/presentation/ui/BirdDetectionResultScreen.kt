package com.riyga.identifier.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.riyga.identifier.data.models.LocationData
import com.riyga.identifier.presentation.models.LocationInfo
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirdDetectionResultScreen(
    navController: NavController,
    detectedBirds: List<DetectedBird>,
    location: LocationData?,
    locationInfo: LocationInfo?,
    audioFilePath: String?,
    viewModel: BirdDetectionResultViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    
    // Handle back press - go to start screen
    BackHandler {
        navController.popBackStack(AppDestination.Start, inclusive = false)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detection Results") },
                navigationIcon = {
                    IconButton(onClick = { 
                        navController.popBackStack(AppDestination.Start, inclusive = false)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Start")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Location info card
            if (location != null) {
                LocationInfoCard(
                    location = location,
                    locationInfo = locationInfo,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Results header
            Text(
                text = if (detectedBirds.isEmpty()) "No Birds Detected" else "Detected Birds (${detectedBirds.size})",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (detectedBirds.isEmpty()) {
                // Empty state
                EmptyDetectionCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                // Birds list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(detectedBirds) { bird ->
                        DetectedBirdCard(bird = bird)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            ActionButtons(
                hasDetections = detectedBirds.isNotEmpty(),
                isSaving = state.isSaving,
                isSaved = state.isSaved,
                saveError = state.saveError,
                onSave = { viewModel.saveRecord(detectedBirds, location, locationInfo, audioFilePath) },
                onNewRecording = {
                    navController.popBackStack(AppDestination.Start, inclusive = false)
                }
            )
        }
    }
}

@Composable
fun LocationInfoCard(
    location: LocationData,
    locationInfo: LocationInfo?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📍 Recording Location",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
//            if (locationInfo != null) {
//                if (!locationInfo.place.isNullOrEmpty()) {
//                    Text(
//                        text = locationInfo.place,
//                        style = MaterialTheme.typography.bodyMedium
//                    )
//                }
//                if (!locationInfo.location.isNullOrEmpty()) {
//                    Text(
//                        text = locationInfo.location,
//                        style = MaterialTheme.typography.bodySmall,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//            }
            
            Text(
                text = "${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DetectedBirdCard(
    bird: DetectedBird,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        text = bird.name.substringBefore("_"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Confidence badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when {
                    bird.confidence >= 0.8f -> MaterialTheme.colorScheme.primaryContainer
                    bird.confidence >= 0.6f -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.tertiaryContainer
                }
            ) {
                Text(
                    text = "${(bird.confidence * 100).toInt()}%",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EmptyDetectionCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🔇",
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Birds Detected",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Try recording in a different location or at a different time when birds are more active.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ActionButtons(
    hasDetections: Boolean,
    isSaving: Boolean,
    isSaved: Boolean,
    saveError: String?,
    onSave: () -> Unit,
    onNewRecording: () -> Unit
) {
    Column {
        if (saveError != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = saveError,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Save button (only show if there are detections and not saved yet)
            if (hasDetections && !isSaved) {
                Button(
                    onClick = onSave,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving...")
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Birds")
                    }
                }
            }
            
            // Success indicator (show when saved)
            if (isSaved) {
                Button(
                    onClick = { },
                    enabled = false,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saved!")
                }
            }
            
            // New recording button
            OutlinedButton(
                onClick = onNewRecording,
                modifier = Modifier.weight(1f)
            ) {
                Text("New Recording")
            }
        }
    }
}