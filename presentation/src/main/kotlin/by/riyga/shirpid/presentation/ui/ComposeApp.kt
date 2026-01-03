package by.riyga.shirpid.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import by.riyga.shirpid.data.preferences.AppPreferences
import by.riyga.shirpid.presentation.ui.record.RecordScreen
import by.riyga.shirpid.presentation.ui.history.BirdHistoryScreen
import by.riyga.shirpid.presentation.ui.progress.ProgressScreen
import by.riyga.shirpid.presentation.ui.settings.LicenseScreen
import by.riyga.shirpid.presentation.ui.settings.SettingsScreen
import by.riyga.shirpid.presentation.ui.start.StartScreen
import by.riyga.shirpid.presentation.theme.AppTheme
import by.riyga.shirpid.presentation.ui.file.FileScreen
import by.riyga.shirpid.presentation.utils.AnalyticsUtil
import by.riyga.shirpid.presentation.utils.LocalNavController
import by.riyga.shirpid.presentation.utils.updateAppLocale
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ComposeApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val viewModel: ComposeAppViewModel = koinViewModel()
    val effect by viewModel.effect.collectAsStateWithLifecycle(null)

    LaunchedEffect(effect) {
        when (val effectCast = effect) {
            is AppContract.Effect.LanguageUpdated -> {
                context.updateAppLocale(effectCast.code)
            }

            else -> {}
        }
    }

    LaunchedEffect(backStackEntry) {
        val screen = backStackEntry
            ?.destination
            ?.route
            ?.substringAfter("Route.")
            ?.substringBefore("/")

        if (screen != null) {
            AnalyticsUtil.screenView(screen)
        }
    }

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
    data class Record(
        val recordId: Long,
        val fromArchive: Boolean
    ) : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object License : Route

    @Serializable
    data object File : Route
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