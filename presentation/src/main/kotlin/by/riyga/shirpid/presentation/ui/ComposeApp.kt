package by.riyga.shirpid.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import by.riyga.shirpid.presentation.navigation.AppNavHost
import by.riyga.shirpid.presentation.theme.AppTheme
import by.riyga.shirpid.presentation.utils.AnalyticsUtil
import by.riyga.shirpid.presentation.utils.LocalNavController
import by.riyga.shirpid.presentation.utils.updateAppLocale
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