package by.riyga.shirpid.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import data.models.LocationData
import by.riyga.shirpid.presentation.ui.detection_result.BirdDetectionResultScreen
import by.riyga.shirpid.presentation.ui.history.BirdHistoryScreen
import by.riyga.shirpid.presentation.ui.progress.ProgressScreen
import by.riyga.shirpid.presentation.ui.settings.LicenseScreen
import by.riyga.shirpid.presentation.ui.settings.SettingsScreen
import by.riyga.shirpid.presentation.ui.start.StartScreen
import by.riyga.shirpid.theme.AppTheme
import by.riyga.shirpid.utils.LocalNavController
import kotlinx.serialization.Serializable

@Composable
fun ComposeApp() {
    val navController = rememberNavController()

    AppTheme {
        CompositionLocalProvider(LocalNavController provides navController) {
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
    data object Progress : Route

    @Serializable
    data object BirdHistory : Route

    @Serializable
    data class DetectionResult(
        val recordId: Long,
        val fromHistory: Boolean
    ) : Route

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
                    navController.navigate(Route.Progress)
                },
                onShowHistory = {
                    navController.navigate(
                        Route.BirdHistory
                    )
                }
            )
        }

        composable<Route.Progress> { backStackEntry ->
            ProgressScreen()
        }

        composable<Route.BirdHistory> {
            BirdHistoryScreen(navController = navController)
        }

        composable<Route.DetectionResult> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.DetectionResult>()
            BirdDetectionResultScreen(
                recordId = route.recordId,
                fromHistory = route.fromHistory
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