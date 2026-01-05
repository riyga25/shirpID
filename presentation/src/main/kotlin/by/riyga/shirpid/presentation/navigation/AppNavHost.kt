package by.riyga.shirpid.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import by.riyga.shirpid.presentation.ui.file.FileScreen
import by.riyga.shirpid.presentation.ui.history.BirdHistoryScreen
import by.riyga.shirpid.presentation.ui.progress.ProgressScreen
import by.riyga.shirpid.presentation.ui.record.RecordScreen
import by.riyga.shirpid.presentation.ui.settings.LicenseScreen
import by.riyga.shirpid.presentation.ui.settings.SettingsScreen
import by.riyga.shirpid.presentation.ui.start.StartScreen

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

        composable<Route.Record> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.Record>()
            RecordScreen(
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

        composable<Route.File> {
            FileScreen()
        }
    }
}