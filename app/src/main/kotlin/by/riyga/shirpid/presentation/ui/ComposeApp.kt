package by.riyga.shirpid.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import by.riyga.shirpid.data.models.LocationData
import by.riyga.shirpid.presentation.models.LocationInfo
import by.riyga.shirpid.presentation.ui.detection_result.BirdDetectionResultScreen
import by.riyga.shirpid.presentation.ui.detection_result.DetectedBird
import by.riyga.shirpid.presentation.ui.history.BirdHistoryScreen
import by.riyga.shirpid.presentation.ui.progress.ProgressScreen
import by.riyga.shirpid.presentation.ui.record_detail.RecordDetailScreen
import by.riyga.shirpid.presentation.ui.settings.LicenseScreen
import by.riyga.shirpid.presentation.ui.settings.SettingsScreen
import by.riyga.shirpid.presentation.ui.start.StartScreen
import by.riyga.shirpid.theme.AppTheme
import by.riyga.shirpid.utils.LocalNavController
import by.riyga.shirpid.utils.composableWithArgs
import by.riyga.shirpid.utils.navType
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