package com.riyga.identifier.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.riyga.identifier.data.models.LocationData
import com.riyga.identifier.presentation.models.LocationInfo
import com.riyga.identifier.presentation.ui.detection_result.BirdDetectionResultScreen
import com.riyga.identifier.presentation.ui.detection_result.DetectedBird
import com.riyga.identifier.presentation.ui.history.BirdHistoryScreen
import com.riyga.identifier.presentation.ui.progress.ProgressScreen
import com.riyga.identifier.presentation.ui.record_detail.RecordDetailScreen
import com.riyga.identifier.presentation.ui.settings.LicenseScreen
import com.riyga.identifier.presentation.ui.settings.SettingsScreen
import com.riyga.identifier.presentation.ui.start.StartScreen
import com.riyga.identifier.theme.AppTheme
import com.riyga.identifier.utils.LocalNavController
import com.riyga.identifier.utils.composableWithArgs
import com.riyga.identifier.utils.navType
import kotlinx.serialization.Serializable

@Composable
fun ComposeApp() {
    val navController = rememberNavController()

    AppTheme {
        CompositionLocalProvider(
            LocalNavController provides navController
        ) {
            AppNavHost(
                navController = navController
            )
        }
    }
}

sealed interface Route {
    @Serializable
    data object Start : Route

    @Serializable
    data class Progress(val location: LocationData) : Route

    @Serializable
    data object BirdHistory : Route

    @Serializable
    data class BirdDetectionResult(
        val detectedBirds: List<DetectedBird>,
        val location: LocationData?,
        val locationInfo: LocationInfo?,
        val audioFilePath: String?
    ) : Route

    @Serializable
    data class RecordDetail(val recordId: Long) : Route
    
    @Serializable
    data object Settings : Route

    @Serializable
    data object License : Route
}

@Composable
fun AppNavHost(
    navController: NavHostController
) {
    NavHost(navController = navController, startDestination = Route.Start) {
        composable<Route.Start> {
            StartScreen(
                navController = navController,
                onStart = { location ->
                    navController.navigate(
                        Route.Progress(location)
                    )
                },
                onShowHistory = {
                    navController.navigate(
                        Route.BirdHistory
                    )
                }
            )
        }

        composableWithArgs<Route.Progress>(
            navType<LocationData>()
        ) { backStackEntry ->
            ProgressScreen(
                location = backStackEntry.toRoute<Route.Progress>().location,
                onNavigateToResults = { detectedBirds, location, locationInfo, audioFilePath ->
                    navController.navigate(
                        Route.BirdDetectionResult(
                            detectedBirds = detectedBirds,
                            location = location,
                            locationInfo = locationInfo,
                            audioFilePath = audioFilePath
                        )
                    )
                }
            )
        }

        composable<Route.BirdHistory> {
            BirdHistoryScreen(navController = navController)
        }

        composableWithArgs<Route.BirdDetectionResult>(
            navType<List<DetectedBird>>(),
            navType<LocationData?>(nullable = true),
            navType<LocationInfo?>(nullable = true)
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<Route.BirdDetectionResult>()
            BirdDetectionResultScreen(
                navController = navController,
                detectedBirds = route.detectedBirds,
                location = route.location,
                locationInfo = route.locationInfo,
                audioFilePath = route.audioFilePath
            )
        }

        composableWithArgs<Route.RecordDetail>(
            navType<Long>()
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<Route.RecordDetail>()
            val recordId = route.recordId

            // We need to fetch the record from the database
            // For now, we'll pass a placeholder and fetch the record in the detail screen
            // In a real implementation, you'd fetch the record here and pass it to the screen
            RecordDetailScreen(
                navController = navController,
                recordId = recordId
            )
        }
        
        composable<Route.Settings> {
            SettingsScreen(navController = navController)
        }

        composable<Route.License> {
            LicenseScreen()
        }
    }
}