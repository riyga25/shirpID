package by.riyga.shirpid.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import by.riyga.shirpid.presentation.ui.detection_result.BirdDetectionResultScreen
import by.riyga.shirpid.presentation.ui.history.BirdHistoryScreen
import by.riyga.shirpid.presentation.ui.progress.ProgressScreen
import by.riyga.shirpid.presentation.ui.settings.LicenseScreen
import by.riyga.shirpid.presentation.ui.settings.SettingsScreen
import by.riyga.shirpid.presentation.ui.start.StartScreen
import by.riyga.shirpid.presentation.theme.AppTheme
import by.riyga.shirpid.presentation.utils.LocalNavController
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
    data object Archive : Route

    @Serializable
    data class DetectionResult(
        val recordId: Long,
        val fromArchive: Boolean
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
    NavHost(
        navController = navController,
        startDestination = Route.Start
    ) {
        composable<Route.Start> {
            StartScreen()
        }

        composable<Route.Progress> { backStackEntry ->
            ProgressScreen()
        }

        composable<Route.Archive> {
            BirdHistoryScreen()
        }

        composable<Route.DetectionResult> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.DetectionResult>()
            BirdDetectionResultScreen(
                recordId = route.recordId,
                fromArchive = route.fromArchive
            )
        }

        composable<Route.Settings> {
            SettingsScreen()
        }

        composable<Route.License> {
            LicenseScreen()
        }
    }
}